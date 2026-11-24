package com.examease.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ExamAttempt implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String studentId;
    private String examId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime deadline;
    private boolean submitted;
    private Map<String, String> selectedAnswers; // questionId -> chosenOption ("A", "B", "C", "D")
    private Set<String> markedForReview; // questionIds

    public ExamAttempt() {
        this.id = UUID.randomUUID().toString();
        this.selectedAnswers = new HashMap<>();
        this.markedForReview = new HashSet<>();
        this.submitted = false;
    }

    public ExamAttempt(String studentId, String examId, LocalDateTime startTime, LocalDateTime deadline) {
        this.id = UUID.randomUUID().toString();
        this.studentId = studentId;
        this.examId = examId;
        this.startTime = startTime;
        this.deadline = deadline;
        this.submitted = false;
        this.selectedAnswers = new HashMap<>();
        this.markedForReview = new HashSet<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getExamId() {
        return examId;
    }

    public void setExamId(String examId) {
        this.examId = examId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public void setSubmitted(boolean submitted) {
        this.submitted = submitted;
    }

    public Map<String, String> getSelectedAnswers() {
        if (selectedAnswers == null) {
            selectedAnswers = new HashMap<>();
        }
        return selectedAnswers;
    }

    public void setSelectedAnswers(Map<String, String> selectedAnswers) {
        this.selectedAnswers = selectedAnswers;
    }

    public Set<String> getMarkedForReview() {
        if (markedForReview == null) {
            markedForReview = new HashSet<>();
        }
        return markedForReview;
    }

    public void setMarkedForReview(Set<String> markedForReview) {
        this.markedForReview = markedForReview;
    }

    public void recordAnswer(String questionId, String answer) {
        if (selectedAnswers == null) {
            selectedAnswers = new HashMap<>();
        }
        if (answer == null || answer.isBlank()) {
            selectedAnswers.remove(questionId);
        } else {
            selectedAnswers.put(questionId, answer);
        }
    }

    public void toggleMarkForReview(String questionId, boolean mark) {
        if (markedForReview == null) {
            markedForReview = new HashSet<>();
        }
        if (mark) {
            markedForReview.add(questionId);
        } else {
            markedForReview.remove(questionId);
        }
    }
}
