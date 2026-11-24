package com.examease.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public class Result implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String studentId;
    private String examId;
    private int totalQuestions;
    private int correctAnswers;
    private int incorrectAnswers;
    private int unansweredQuestions;
    private int marksObtained;
    private int maximumMarks;
    private double percentage;
    private String status; // "PASS" or "FAIL"
    private LocalDateTime submissionDate;

    public Result() {
        this.id = UUID.randomUUID().toString();
        this.submissionDate = LocalDateTime.now();
    }

    public Result(String studentId, String examId, int totalQuestions, int correctAnswers,
                  int incorrectAnswers, int unansweredQuestions, int marksObtained,
                  int maximumMarks, double percentage, String status, LocalDateTime submissionDate) {
        this.id = UUID.randomUUID().toString();
        this.studentId = studentId;
        this.examId = examId;
        this.totalQuestions = totalQuestions;
        this.correctAnswers = correctAnswers;
        this.incorrectAnswers = incorrectAnswers;
        this.unansweredQuestions = unansweredQuestions;
        this.marksObtained = marksObtained;
        this.maximumMarks = maximumMarks;
        this.percentage = Math.round(percentage * 100.0) / 100.0;
        this.status = status;
        this.submissionDate = submissionDate != null ? submissionDate : LocalDateTime.now();
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

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public int getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(int correctAnswers) {
        this.correctAnswers = correctAnswers;
    }

    public int getIncorrectAnswers() {
        return incorrectAnswers;
    }

    public void setIncorrectAnswers(int incorrectAnswers) {
        this.incorrectAnswers = incorrectAnswers;
    }

    public int getUnansweredQuestions() {
        return unansweredQuestions;
    }

    public void setUnansweredQuestions(int unansweredQuestions) {
        this.unansweredQuestions = unansweredQuestions;
    }

    public int getMarksObtained() {
        return marksObtained;
    }

    public void setMarksObtained(int marksObtained) {
        this.marksObtained = marksObtained;
    }

    public int getMaximumMarks() {
        return maximumMarks;
    }

    public void setMaximumMarks(int maximumMarks) {
        this.maximumMarks = maximumMarks;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = Math.round(percentage * 100.0) / 100.0;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(LocalDateTime submissionDate) {
        this.submissionDate = submissionDate;
    }
}
