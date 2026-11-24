package com.examease.exception;

public class InvalidExamDataException extends RuntimeException {
    public InvalidExamDataException(String message) {
        super(message);
    }
}
