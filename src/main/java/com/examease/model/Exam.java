
package com.examease.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Exam implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String title;
    private String description;
    private int duration; // in minutes
    private int totalMarks;
    private int passingMarks;
    private boolean published;
    private List<Question> questions;

    // Access control fields
    // ALL: Every registered student can access the exam.
    // SELECTED: Only assigned students can access the exam.
    private String accessType = "ALL";
    private Set<String> allowedStudentIds = new HashSet<>();

    public Exam() {
        this.id = UUID.randomUUID().toString();
        this.questions = new ArrayList<>();
        this.published = false;
        this.duration = 15;
        this.passingMarks = 3;
    }

    public Exam(String title, String description, int duration,
                int totalMarks, int passingMarks, boolean published) {

        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.duration = duration;
        this.totalMarks = totalMarks;
        this.passingMarks = passingMarks;
        this.published = published;
        this.questions = new ArrayList<>();
    }

    public Exam(String id, String title, String description,
                int duration, int totalMarks, int passingMarks,
                boolean published, List<Question> questions) {

        this.id = (id != null && !id.isBlank())
                ? id : UUID.randomUUID().toString();

        this.title = title;
        this.description = description;
        this.duration = duration;
        this.totalMarks = totalMarks;
        this.passingMarks = passingMarks;
        this.published = published;
        this.questions = questions != null
                ? questions : new ArrayList<>();
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(int totalMarks) {
        this.totalMarks = totalMarks;
    }

    public int getPassingMarks() {
        return passingMarks;
    }

    public void setPassingMarks(int passingMarks) {
        this.passingMarks = passingMarks;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public List<Question> getQuestions() {
        if (questions == null) {
            questions = new ArrayList<>();
        }
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    // Question Management

    public void addQuestion(Question question) {
        if (this.questions == null) {
            this.questions = new ArrayList<>();
        }

        this.questions.add(question);
        recalculateTotalMarks();
    }

    public void removeQuestion(String questionId) {
        if (this.questions != null) {
            this.questions.removeIf(
                    q -> q.getId().equals(questionId)
            );

            recalculateTotalMarks();
        }
    }

    public void recalculateTotalMarks() {
        if (this.questions != null && !this.questions.isEmpty()) {
            this.totalMarks = this.questions.stream()
                    .mapToInt(Question::getMarks)
                    .sum();
        }
    }

    // Access Control

    public String getAccessType() {
        return (accessType != null) ? accessType : "ALL";
    }

    public void setAccessType(String accessType) {
        this.accessType = (accessType != null)
                ? accessType : "ALL";
    }

    public Set<String> getAllowedStudentIds() {
        if (allowedStudentIds == null) {
            allowedStudentIds = new HashSet<>();
        }

        return allowedStudentIds;
    }

    public void setAllowedStudentIds(Set<String> allowedStudentIds) {
        this.allowedStudentIds = (allowedStudentIds != null)
                ? allowedStudentIds : new HashSet<>();
    }

    /**
     * Checks whether a student is allowed to attempt this exam.
     *
     * ALL      -> Every registered student can access.
     * SELECTED -> Only assigned students can access.
     */
    public boolean isAccessible(String studentId) {

        if ("ALL".equalsIgnoreCase(getAccessType())) {
            return true;
        }

        if (studentId == null || studentId.isBlank()) {
            return false;
        }

        return "SELECTED".equalsIgnoreCase(getAccessType())
                && getAllowedStudentIds().contains(studentId);
    }

    /**
     * Custom deserialization for backward compatibility.
     * Older serialized Exam objects will default to ALL access.
     */
    private void readObject(ObjectInputStream ois)
            throws IOException, ClassNotFoundException {

        ois.defaultReadObject();

        if (this.accessType == null) {
            this.accessType = "ALL";
        }

        if (this.allowedStudentIds == null) {
            this.allowedStudentIds = new HashSet<>();
        }

        if (this.questions == null) {
            this.questions = new ArrayList<>();
        }
    }
}
