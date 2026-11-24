package com.examease.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String type; // "EXAM_SUBMITTED", "STUDENT_REGISTERED", "TIMER_EXPIRED", "SYSTEM"
    private String message;
    private LocalDateTime timestamp;
    private boolean read;

    public Notification() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.read = false;
    }

    public Notification(String type, String message) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.read = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
