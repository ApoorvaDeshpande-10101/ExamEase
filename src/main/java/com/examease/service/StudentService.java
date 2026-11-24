
package com.examease.service;

import com.examease.exception.ExamAlreadySubmittedException;
import com.examease.exception.ExamNotFoundException;
import com.examease.exception.UnauthorizedException;
import com.examease.model.*;
import com.examease.repository.AttemptRepository;
import com.examease.repository.ExamRepository;
import com.examease.repository.ResultRepository;
import com.examease.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudentService {

    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final AttemptRepository attemptRepository;
    private final ResultRepository resultRepository;
    private final BackgroundTaskService backgroundTaskService;

    public StudentService(UserRepository userRepository,
                          ExamRepository examRepository,
                          AttemptRepository attemptRepository,
                          ResultRepository resultRepository,
                          BackgroundTaskService backgroundTaskService) {
        this.userRepository = userRepository;
        this.examRepository = examRepository;
        this.attemptRepository = attemptRepository;
        this.resultRepository = resultRepository;
        this.backgroundTaskService = backgroundTaskService;
    }

    public List<User> getAllStudents() {
        return userRepository.findAll().stream()
                .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole()))
                .collect(Collectors.toList());
    }

    public User getStudentById(String id) {
        User student = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Student not found with ID: " + id
                ));

        if (!"STUDENT".equalsIgnoreCase(student.getRole())) {
            throw new IllegalArgumentException("The requested account is not a student.");
        }

        return student;
    }

    /**
     * Deletes a student and the student's related examination data.
     * Admin accounts, exams and notifications are not deleted.
     */
    public void deleteStudent(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            throw new IllegalArgumentException("Invalid student ID.");
        }

        backgroundTaskService.executeWithStudentDataLock(() -> {
            deleteStudentLocked(studentId);
            return null;
        });
    }

    /**
     * This method must be called while holding the shared student data lock.
     */
    private void deleteStudentLocked(String studentId) {

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Student not found. The account may already have been deleted."
                ));

        // Never allow an admin account to be deleted through this operation.
        if (!"STUDENT".equalsIgnoreCase(student.getRole())) {
            throw new IllegalArgumentException("Only student accounts can be deleted.");
        }

        // Collect all existing records before changing the repositories.
        List<ExamAttempt> remainingAttempts = attemptRepository.findAll().stream()
                .filter(attempt -> !studentId.equals(attempt.getStudentId()))
                .collect(Collectors.toList());

        List<Result> remainingResults = resultRepository.findAll().stream()
                .filter(result -> !studentId.equals(result.getStudentId()))
                .collect(Collectors.toList());

        List<Exam> allExams = examRepository.findAll();

        // Remove the student's attempts and results.
        attemptRepository.saveAll(remainingAttempts);
        resultRepository.saveAll(remainingResults);

        // Remove the student from exams that have selected-student access.
        for (Exam exam : allExams) {
            if (exam.getAllowedStudentIds() != null) {
                exam.getAllowedStudentIds().remove(studentId);
            }
        }

        // Save the exams without changing or deleting the exams themselves.
        examRepository.saveAll(allExams);

        // Delete the user account only after related records have been updated.
        userRepository.deleteById(studentId);
    }

    public Map<String, Object> getStudentDashboardStats(String studentId) {
        List<Result> studentResults = resultRepository.findByStudentId(studentId);

        int totalAttempts = studentResults.size();

        double averageScore = studentResults.stream()
                .mapToDouble(Result::getPercentage)
                .average()
                .orElse(0.0);

        averageScore = Math.round(averageScore * 100.0) / 100.0;

        double bestScore = studentResults.stream()
                .mapToDouble(Result::getPercentage)
                .max()
                .orElse(0.0);

        bestScore = Math.round(bestScore * 100.0) / 100.0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAttempts", totalAttempts);
        stats.put("averageScore", averageScore);
        stats.put("bestScore", bestScore);

        return stats;
    }

    public List<Result> getStudentRecentResults(String studentId, int limit) {
        return resultRepository.findByStudentId(studentId).stream()
                .sorted((a, b) -> b.getSubmissionDate().compareTo(a.getSubmissionDate()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public boolean hasStudentAttempted(String studentId, String examId) {
        return resultRepository.findByStudentId(studentId).stream()
                .anyMatch(r -> r.getExamId().equals(examId));
    }

    public ExamAttempt startOrResumeExam(String studentId, String examId) {
        return backgroundTaskService.executeWithStudentDataLock(
                () -> startOrResumeExamLocked(studentId, examId)
        );
    }

    private ExamAttempt startOrResumeExamLocked(String studentId, String examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamNotFoundException("Exam not found: " + examId));

        // Verify that the user exists and is a student.
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new UnauthorizedException("Student account not found."));

        if (!"STUDENT".equalsIgnoreCase(student.getRole())) {
            throw new UnauthorizedException("Only students can attempt examinations.");
        }

        // Check whether the exam is published.
        if (!exam.isPublished()) {
            throw new UnauthorizedException(
                    "This examination is not currently available for attempts."
            );
        }

        // Check whether this student is allowed to access the exam.
        if (!exam.isAccessible(studentId)) {
            throw new UnauthorizedException("You are not assigned to this examination.");
        }

        // Check if student already submitted this exam.
        if (hasStudentAttempted(studentId, examId)) {
            throw new ExamAlreadySubmittedException(
                    "You have already completed and submitted this examination."
            );
        }

        // Check if an existing unsubmitted attempt exists.
        Optional<ExamAttempt> existingOpt =
                attemptRepository.findActiveAttempt(studentId, examId);

        if (existingOpt.isPresent()) {
            ExamAttempt attempt = existingOpt.get();

            // If expired already, auto-submit and throw.
            if (LocalDateTime.now().isAfter(attempt.getDeadline())) {
                submitExam(attempt.getId(), studentId);

                throw new ExamAlreadySubmittedException(
                        "The deadline for this examination has expired."
                );
            }

            return attempt;
        }

        // Create new attempt.
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusMinutes(exam.getDuration());

        ExamAttempt newAttempt = new ExamAttempt(studentId, examId, now, deadline);
        attemptRepository.save(newAttempt);

        backgroundTaskService.notifyEvent(
                "EXAM_STARTED",
                "Student ID " + studentId + " started exam: " + exam.getTitle()
        );

        return newAttempt;
    }

    public ExamAttempt getAttempt(String attemptId, String studentId) {
        ExamAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Exam attempt not found with ID: " + attemptId
                ));

        if (!attempt.getStudentId().equals(studentId)) {
            throw new UnauthorizedException(
                    "Access denied. You are not authorized to view this exam attempt."
            );
        }

        return attempt;
    }

    public void saveAnswer(String attemptId,
                           String studentId,
                           String questionId,
                           String answer,
                           Boolean markForReview) {

        ExamAttempt attempt = getAttempt(attemptId, studentId);

        if (attempt.isSubmitted()) {
            throw new ExamAlreadySubmittedException(
                    "This examination has already been submitted."
            );
        }

        // Check if deadline passed.
        if (LocalDateTime.now().isAfter(attempt.getDeadline())) {
            submitExam(attemptId, studentId);

            throw new ExamAlreadySubmittedException(
                    "Exam duration has expired. The exam has been automatically submitted."
            );
        }

        // Use multithreaded asynchronous background saving.
        backgroundTaskService.saveProgressAsync(
                attemptId,
                questionId,
                answer,
                markForReview
        );
    }

    public Result submitExam(String attemptId, String studentId) {
        return backgroundTaskService.executeWithStudentDataLock(
                () -> submitExamLocked(attemptId, studentId)
        );
    }

    private Result submitExamLocked(String attemptId, String studentId) {
        ExamAttempt attempt = getAttempt(attemptId, studentId);

        if (attempt.isSubmitted()) {
            return resultRepository.findLatestByStudentIdAndExamId(
                            studentId,
                            attempt.getExamId()
                    )
                    .orElseThrow(() -> new IllegalStateException(
                            "Attempt marked submitted but result not found."
                    ));
        }

        Exam exam = examRepository.findById(attempt.getExamId())
                .orElseThrow(() -> new ExamNotFoundException(
                        "Exam not found: " + attempt.getExamId()
                ));

        attempt.setSubmitted(true);
        attempt.setEndTime(LocalDateTime.now());
        attemptRepository.save(attempt);

        Result result = backgroundTaskService.evaluateAttempt(attempt, exam);
        resultRepository.save(result);

        // Fetch user name for notification.
        String studentName = userRepository.findById(studentId)
                .map(User::getName)
                .orElse(studentId);

        backgroundTaskService.notifyEvent(
                "EXAM_SUBMITTED",
                "Student " + studentName
                        + " submitted exam '" + exam.getTitle()
                        + "' with score: " + result.getMarksObtained()
                        + "/" + result.getMaximumMarks()
                        + " (" + result.getStatus() + ")"
        );

        return result;
    }
}
