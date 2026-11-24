
package com.examease.service;

import com.examease.exception.ExamNotFoundException;
import com.examease.exception.InvalidExamDataException;
import com.examease.model.Exam;
import com.examease.model.Question;
import com.examease.model.User;
import com.examease.repository.ExamRepository;
import com.examease.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ExamService {

    private final ExamRepository examRepository;
    private final BackgroundTaskService backgroundTaskService;
    private final UserRepository userRepository;

    public ExamService(ExamRepository examRepository,
                       BackgroundTaskService backgroundTaskService,
                       UserRepository userRepository) {
        this.examRepository = examRepository;
        this.backgroundTaskService = backgroundTaskService;
        this.userRepository = userRepository;
    }

    public List<Exam> getAllExams() {
        return examRepository.findAll();
    }

    public List<Exam> getPublishedExams() {
        return examRepository.findByPublished(true);
    }

    public Exam getExamById(String id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new ExamNotFoundException(
                        "Examination with ID '" + id + "' not found."));
    }

    public Exam createExam(String title, String description,
                           int duration, int passingMarks) {
        return createExam(title, description, duration, passingMarks, "ALL", null);
    }

    public Exam createExam(String title, String description,
                           int duration, int passingMarks,
                           String accessType, List<String> allowedStudentIds) {

        validateExamDetails(title, duration, passingMarks);

        String resolvedType = resolveAccessType(accessType);

        Exam exam = new Exam(
                title.trim(),
                description != null ? description.trim() : "",
                duration,
                0,
                passingMarks,
                false
        );

        exam.setAccessType(resolvedType);

        if ("SELECTED".equals(resolvedType)) {
            exam.setAllowedStudentIds(
                    new HashSet<>(validateAndFilterStudentIds(allowedStudentIds))
            );
        } else {
            exam.setAllowedStudentIds(new HashSet<>());
        }

        examRepository.save(exam);

        backgroundTaskService.notifyEvent(
                "SYSTEM",
                "New examination created: " + exam.getTitle()
        );

        return exam;
    }

    public Exam updateExam(String id, String title, String description,
                           int duration, int passingMarks) {
        return updateExam(id, title, description, duration, passingMarks, null, null);
    }

    public Exam updateExam(String id, String title, String description,
                           int duration, int passingMarks,
                           String accessType, List<String> allowedStudentIds) {

        Exam exam = getExamById(id);

        validateExamDetails(title, duration, passingMarks);

        exam.setTitle(title.trim());
        exam.setDescription(description != null ? description.trim() : "");
        exam.setDuration(duration);
        exam.setPassingMarks(passingMarks);
        exam.recalculateTotalMarks();

        // Update access settings only when supplied.
        if (accessType != null) {

            String resolvedType = resolveAccessType(accessType);

            exam.setAccessType(resolvedType);

            if ("SELECTED".equals(resolvedType)) {

                // Null or empty list means no students are selected.
                exam.setAllowedStudentIds(
                        new HashSet<>(validateAndFilterStudentIds(allowedStudentIds))
                );

            } else {

                // ALL access must not retain old student restrictions.
                exam.setAllowedStudentIds(new HashSet<>());
            }
        }

        examRepository.save(exam);

        return exam;
    }

    public void deleteExam(String id) {
        Exam exam = getExamById(id);

        examRepository.deleteById(id);

        backgroundTaskService.notifyEvent(
                "SYSTEM",
                "Examination deleted: " + exam.getTitle()
        );
    }

    public Exam togglePublishStatus(String id) {
        Exam exam = getExamById(id);

        if (!exam.isPublished()
                && (exam.getQuestions() == null || exam.getQuestions().isEmpty())) {

            throw new InvalidExamDataException(
                    "Cannot publish an examination with no questions. Please add questions first."
            );
        }

        // SELECTED access requires at least one assigned student.
        if (!exam.isPublished()
                && "SELECTED".equalsIgnoreCase(exam.getAccessType())
                && (exam.getAllowedStudentIds() == null
                || exam.getAllowedStudentIds().isEmpty())) {

            throw new InvalidExamDataException(
                    "Cannot publish: no students are assigned for this SELECTED access examination. Please assign at least one student first."
            );
        }

        exam.setPublished(!exam.isPublished());

        examRepository.save(exam);

        String action = exam.isPublished() ? "published" : "unpublished";

        backgroundTaskService.notifyEvent(
                "SYSTEM",
                "Examination " + action + ": " + exam.getTitle()
        );

        return exam;
    }

    public Question addQuestionToExam(String examId, String questionText,
                                       String optionA, String optionB,
                                       String optionC, String optionD,
                                       String correctAnswer, int marks) {

        Exam exam = getExamById(examId);

        validateQuestion(
                questionText, optionA, optionB, optionC,
                optionD, correctAnswer, marks
        );

        Question question = new Question(
                questionText.trim(),
                optionA.trim(),
                optionB.trim(),
                optionC.trim(),
                optionD.trim(),
                correctAnswer.trim().toUpperCase(),
                marks
        );

        exam.addQuestion(question);
        examRepository.save(exam);

        return question;
    }

    public Question updateQuestion(String examId, String questionId,
                                    String questionText, String optionA,
                                    String optionB, String optionC,
                                    String optionD, String correctAnswer,
                                    int marks) {

        Exam exam = getExamById(examId);

        validateQuestion(
                questionText, optionA, optionB, optionC,
                optionD, correctAnswer, marks
        );

        Question target = exam.getQuestions().stream()
                .filter(q -> q.getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new InvalidExamDataException(
                        "Question with ID '" + questionId + "' not found in exam."
                ));

        target.setQuestionText(questionText.trim());
        target.setOptionA(optionA.trim());
        target.setOptionB(optionB.trim());
        target.setOptionC(optionC.trim());
        target.setOptionD(optionD.trim());
        target.setCorrectAnswer(correctAnswer.trim().toUpperCase());
        target.setMarks(marks);

        exam.recalculateTotalMarks();
        examRepository.save(exam);

        return target;
    }

    public void deleteQuestion(String examId, String questionId) {
        Exam exam = getExamById(examId);

        exam.removeQuestion(questionId);
        examRepository.save(exam);
    }

    private String resolveAccessType(String accessType) {

        if (accessType == null || accessType.isBlank()
                || "ALL".equalsIgnoreCase(accessType)) {
            return "ALL";
        }

        if ("SELECTED".equalsIgnoreCase(accessType)) {
            return "SELECTED";
        }

        throw new InvalidExamDataException(
                "Invalid examination access type. Choose ALL or SELECTED."
        );
    }

    private void validateExamDetails(String title, int duration, int passingMarks) {

        if (title == null || title.trim().isEmpty()) {
            throw new InvalidExamDataException(
                    "Examination title cannot be empty."
            );
        }

        if (duration <= 0) {
            throw new InvalidExamDataException(
                    "Duration must be at least 1 minute."
            );
        }

        if (passingMarks < 0) {
            throw new InvalidExamDataException(
                    "Passing marks cannot be negative."
            );
        }
    }

    /**
     * Validates that every assigned ID belongs to a registered STUDENT.
     * Null or empty selection returns an empty set.
     */
    private Set<String> validateAndFilterStudentIds(List<String> ids) {

        Set<String> selectedIds = new LinkedHashSet<>();

        if (ids == null || ids.isEmpty()) {
            return selectedIds;
        }

        Set<String> validStudentIds = userRepository.findAll().stream()
                .filter(user -> "STUDENT".equalsIgnoreCase(user.getRole()))
                .map(User::getId)
                .collect(Collectors.toSet());

        for (String studentId : ids) {

            if (studentId == null || studentId.isBlank()
                    || !validStudentIds.contains(studentId)) {

                throw new InvalidExamDataException(
                        "Invalid student selection. Please select only registered students."
                );
            }

            selectedIds.add(studentId);
        }

        return selectedIds;
    }

    /**
     * Checks whether a student is allowed to attempt an examination.
     */
    public boolean isStudentAllowedToAttempt(Exam exam, String studentId) {

        if (exam == null || !exam.isPublished() || studentId == null) {
            return false;
        }

        User student = userRepository.findById(studentId).orElse(null);

        if (student == null
                || !"STUDENT".equalsIgnoreCase(student.getRole())) {
            return false;
        }

        return exam.isAccessible(studentId);
    }

    /**
     * Returns published examinations accessible to the given student.
     */
    public List<Exam> getEligiblePublishedExams(String studentId) {

        return examRepository.findByPublished(true).stream()
                .filter(exam -> isStudentAllowedToAttempt(exam, studentId))
                .collect(Collectors.toList());
    }

    private void validateQuestion(String questionText, String optionA,
                                  String optionB, String optionC,
                                  String optionD, String correctAnswer,
                                  int marks) {

        if (questionText == null || questionText.trim().isEmpty()) {
            throw new InvalidExamDataException(
                    "Question text cannot be empty."
            );
        }

        if (optionA == null || optionA.trim().isEmpty()
                || optionB == null || optionB.trim().isEmpty()
                || optionC == null || optionC.trim().isEmpty()
                || optionD == null || optionD.trim().isEmpty()) {

            throw new InvalidExamDataException(
                    "All 4 options (A, B, C, D) are required."
            );
        }

        if (correctAnswer == null
                || !List.of("A", "B", "C", "D")
                .contains(correctAnswer.trim().toUpperCase())) {

            throw new InvalidExamDataException(
                    "Correct answer must be one of: A, B, C, or D."
            );
        }

        if (marks <= 0) {
            throw new InvalidExamDataException(
                    "Question marks must be at least 1."
            );
        }
    }
}
