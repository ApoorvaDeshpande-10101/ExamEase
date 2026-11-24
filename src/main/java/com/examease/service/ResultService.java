package com.examease.service;

import com.examease.exception.ExamNotFoundException;
import com.examease.exception.UnauthorizedException;
import com.examease.model.*;
import com.examease.repository.AttemptRepository;
import com.examease.repository.ExamRepository;
import com.examease.repository.ResultRepository;
import com.examease.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ResultService {

    private final ResultRepository resultRepository;
    private final ExamRepository examRepository;
    private final UserRepository userRepository;
    private final AttemptRepository attemptRepository;

    public ResultService(ResultRepository resultRepository,
                         ExamRepository examRepository,
                         UserRepository userRepository,
                         AttemptRepository attemptRepository) {
        this.resultRepository = resultRepository;
        this.examRepository = examRepository;
        this.userRepository = userRepository;
        this.attemptRepository = attemptRepository;
    }

    public Result getResultById(String resultId) {
        return resultRepository.findById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("Result not found with ID: " + resultId));
    }

    public List<Result> getResultsByStudentId(String studentId) {
        return resultRepository.findByStudentId(studentId).stream()
                .sorted((a, b) -> b.getSubmissionDate().compareTo(a.getSubmissionDate()))
                .toList();
    }

    public List<Result> getResultsByExamId(String examId) {
        return resultRepository.findByExamId(examId).stream()
                .sorted((a, b) -> b.getSubmissionDate().compareTo(a.getSubmissionDate()))
                .toList();
    }

    public List<Result> getAllResults() {
        return resultRepository.findAll().stream()
                .sorted((a, b) -> b.getSubmissionDate().compareTo(a.getSubmissionDate()))
                .toList();
    }

    /**
     * Builds comprehensive detailed result data for student / admin review
     */
    public Map<String, Object> getResultDetails(String resultId, User loggedInUser) {
        Result result = getResultById(resultId);

        // Security check: only the student who took it or an admin can view
        if (!"ADMIN".equalsIgnoreCase(loggedInUser.getRole()) && !result.getStudentId().equals(loggedInUser.getId())) {
            throw new UnauthorizedException("You are not authorized to view this examination result.");
        }

        Exam exam = examRepository.findById(result.getExamId())
                .orElseThrow(() -> new ExamNotFoundException("Exam not found for this result: " + result.getExamId()));

        User student = userRepository.findById(result.getStudentId()).orElse(null);

        // Find associated attempt to retrieve question-by-question responses
        List<ExamAttempt> attempts = attemptRepository.findByStudentId(result.getStudentId());
        ExamAttempt attempt = attempts.stream()
                .filter(a -> a.getExamId().equals(result.getExamId()) && a.isSubmitted())
                .sorted((a, b) -> b.getEndTime().compareTo(a.getEndTime()))
                .findFirst()
                .orElse(null);

        Map<String, String> answersMap = attempt != null ? attempt.getSelectedAnswers() : Collections.emptyMap();

        // Build question review items
        List<Map<String, Object>> questionReviews = new ArrayList<>();
        if (exam.getQuestions() != null) {
            for (Question q : exam.getQuestions()) {
                Map<String, Object> item = new HashMap<>();
                item.put("question", q);
                String studentAns = answersMap.get(q.getId());
                item.put("studentAnswer", studentAns != null ? studentAns : "Unanswered");
                item.put("correctAnswer", q.getCorrectAnswer());
                boolean isCorrect = studentAns != null && studentAns.equalsIgnoreCase(q.getCorrectAnswer());
                boolean isUnanswered = (studentAns == null || studentAns.isBlank());
                item.put("isCorrect", isCorrect);
                item.put("isUnanswered", isUnanswered);
                item.put("marksAwarded", isCorrect ? q.getMarks() : 0);
                questionReviews.add(item);
            }
        }

        Map<String, Object> details = new HashMap<>();
        details.put("result", result);
        details.put("exam", exam);
        details.put("student", student);
        details.put("questionsReview", questionReviews);

        return details;
    }
}
