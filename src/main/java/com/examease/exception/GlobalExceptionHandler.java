package com.examease.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UserAlreadyExistsException.class)
    public String handleUserAlreadyExists(UserAlreadyExistsException ex, Model model) {
        logger.warn("User already exists: {}", ex.getMessage());
        model.addAttribute("error", ex.getMessage());
        return "register";
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public String handleInvalidCredentials(InvalidCredentialsException ex, Model model) {
        logger.warn("Invalid credentials: {}", ex.getMessage());
        model.addAttribute("error", ex.getMessage());
        return "login";
    }

    @ExceptionHandler(ExamNotFoundException.class)
    public String handleExamNotFound(ExamNotFoundException ex, RedirectAttributes redirectAttributes) {
        logger.warn("Exam not found: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/student/dashboard";
    }

    @ExceptionHandler(ExamAlreadySubmittedException.class)
    public String handleExamAlreadySubmitted(ExamAlreadySubmittedException ex, RedirectAttributes redirectAttributes) {
        logger.warn("Exam already submitted: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/student/results";
    }

    @ExceptionHandler(UnauthorizedException.class)
    public String handleUnauthorized(UnauthorizedException ex, RedirectAttributes redirectAttributes) {
        logger.warn("Unauthorized access: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/login";
    }

    @ExceptionHandler(DataStorageException.class)
    public String handleDataStorageException(DataStorageException ex, Model model) {
        logger.error("Data storage error occurred: ", ex);
        model.addAttribute("errorTitle", "Data Storage Error");
        model.addAttribute("errorMessage", "An error occurred while saving or reading application data. Please try again.");
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex, Model model) {
        logger.error("Unhandled exception caught: ", ex);
        model.addAttribute("errorTitle", "An Unexpected Error Occurred");
        model.addAttribute("errorMessage", ex.getMessage() != null ? ex.getMessage() : "Please contact system administrator.");
        return "error";
    }
}
