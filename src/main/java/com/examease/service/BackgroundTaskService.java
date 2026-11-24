
package com.examease.service;

import com.examease.model.*;
import com.examease.repository.AttemptRepository;
import com.examease.repository.ExamRepository;
import com.examease.repository.NotificationRepository;
import com.examease.repository.ResultRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;

@Service
public class BackgroundTaskService {

    private static final Logger logger = LoggerFactory.getLogger(BackgroundTaskService.class);

    private final AttemptRepository attemptRepository;
    private final ExamRepository examRepository;
    private final ResultRepository resultRepository;
    private final NotificationRepository notificationRepository;

    private ScheduledExecutorService scheduler;
    private ExecutorService autoSaveExecutor;

    public BackgroundTaskService(AttemptRepository attemptRepository,
                                 ExamRepository examRepository,
                                 ResultRepository resultRepository,
                                 NotificationRepository notificationRepository) {
        this.attemptRepository = attemptRepository;
        this.examRepository = examRepository;
        this.resultRepository = resultRepository;
        this.notificationRepository = notificationRepository;
    }

    @PostConstruct
    public void init() {
        // Scheduled background executor for deadline monitoring and periodic tasks
        scheduler = Executors.newScheduledThreadPool(2, new ThreadFactory() {
            private int counter = 1;

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "ExamEase-Scheduler-" + (counter++));
                t.setDaemon(true);
                return t;
            }
        });

        // Background worker pool for non-blocking auto-saving of student examination progress
        autoSaveExecutor = Executors.newFixedThreadPool(2, new ThreadFactory() {
            private int counter = 1;

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "ExamEase-AutoSave-" + (counter++));
                t.setDaemon(true);
                return t;
            }
        });

        // Schedule deadline expiration monitor every 10 seconds
        scheduler.scheduleWithFixedDelay(
                this::checkAndAutoSubmitExpiredAttempts,
                10,
                10,
                TimeUnit.SECONDS
        );

        logger.info("ExamEase Background Tasks & Multithreading services initialized successfully.");
    }

    /**
     * Executes a student-related operation while holding the shared data lock.
     * Used by deletion, exam submission and exam creation/resumption operations.
     */
    public synchronized <T> T executeWithStudentDataLock(Supplier<T> operation) {
        return operation.get();
    }

    /**
     * Multithreaded background auto-save of exam answers.
     * The actual repository operation uses the same lock as student deletion.
     */
    public CompletableFuture<Void> saveProgressAsync(
            String attemptId,
            String questionId,
            String answer,
            Boolean markForReview) {

        return CompletableFuture.runAsync(() -> {
            synchronized (this) {
                try {
                    attemptRepository.findById(attemptId).ifPresent(attempt -> {
                        if (!attempt.isSubmitted()) {
                            if (questionId != null) {
                                attempt.recordAnswer(questionId, answer);

                                if (markForReview != null) {
                                    attempt.toggleMarkForReview(questionId, markForReview);
                                }
                            }

                            attemptRepository.save(attempt);
                        }
                    });
                } catch (Exception e) {
                    logger.error(
                            "Failed to auto-save exam attempt progress in background thread: {}",
                            e.getMessage(),
                            e
                    );
                }
            }
        }, autoSaveExecutor);
    }

    /**
     * Multithreaded background notification dispatcher.
     */
    public void notifyEvent(String type, String message) {
        CompletableFuture.runAsync(() -> {
            try {
                Notification notification = new Notification(type, message);
                notificationRepository.save(notification);

                logger.info("[Background Notification] {}: {}", type, message);
            } catch (Exception e) {
                logger.error("Error saving background notification: {}", e.getMessage());
            }
        }, autoSaveExecutor);
    }

    /**
     * Periodic background task checking for expired exam attempts
     * and automatically evaluating them.
     */
    public synchronized void checkAndAutoSubmitExpiredAttempts() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<ExamAttempt> attempts = attemptRepository.findAll();

            for (ExamAttempt attempt : attempts) {
                if (!attempt.isSubmitted()
                        && attempt.getDeadline() != null
                        && now.isAfter(attempt.getDeadline())) {

                    logger.info(
                            "Attempt ID {} for student {} expired. Auto-submitting in background...",
                            attempt.getId(),
                            attempt.getStudentId()
                    );

                    // Auto-submit attempt
                    attempt.setSubmitted(true);
                    attempt.setEndTime(attempt.getDeadline());
                    attemptRepository.save(attempt);

                    // Compute result if not already computed
                    if (resultRepository.findLatestByStudentIdAndExamId(
                            attempt.getStudentId(),
                            attempt.getExamId()
                    ).isEmpty()) {

                        examRepository.findById(attempt.getExamId()).ifPresent(exam -> {
                            Result result = evaluateAttempt(attempt, exam);
                            resultRepository.save(result);

                            notifyEvent(
                                    "TIMER_EXPIRED",
                                    "Exam '" + exam.getTitle()
                                            + "' auto-submitted for student ID "
                                            + attempt.getStudentId()
                                            + " (Time expired)."
                            );
                        });
                    }
                }
            }
        } catch (Exception e) {
            logger.error(
                    "Error in checkAndAutoSubmitExpiredAttempts: {}",
                    e.getMessage(),
                    e
            );
        }
    }

    /**
     * Server-side evaluation logic.
     */
    public Result evaluateAttempt(ExamAttempt attempt, Exam exam) {
        List<Question> questions = exam.getQuestions();

        int totalQuestions = questions != null ? questions.size() : 0;
        int correctCount = 0;
        int incorrectCount = 0;
        int unansweredCount = 0;
        int marksObtained = 0;
        int maximumMarks = exam.getTotalMarks();

        Map<String, String> selected = attempt.getSelectedAnswers();

        if (questions != null) {
            for (Question q : questions) {
                String selectedOption = selected.get(q.getId());

                if (selectedOption == null || selectedOption.trim().isEmpty()) {
                    unansweredCount++;
                } else if (selectedOption.trim().equalsIgnoreCase(q.getCorrectAnswer().trim())) {
                    correctCount++;
                    marksObtained += q.getMarks();
                } else {
                    incorrectCount++;
                }
            }
        }

        double percentage = maximumMarks > 0
                ? ((double) marksObtained / maximumMarks) * 100.0
                : 0.0;

        String status = marksObtained >= exam.getPassingMarks() ? "PASS" : "FAIL";

        return new Result(
                attempt.getStudentId(),
                exam.getId(),
                totalQuestions,
                correctCount,
                incorrectCount,
                unansweredCount,
                marksObtained,
                maximumMarks,
                percentage,
                status,
                attempt.getEndTime() != null
                        ? attempt.getEndTime()
                        : LocalDateTime.now()
        );
    }

    @PreDestroy
    public void cleanup() {
        logger.info("Shutting down ExamEase background executors gracefully...");

        if (scheduler != null) {
            scheduler.shutdown();

            try {
                if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (autoSaveExecutor != null) {
            autoSaveExecutor.shutdown();

            try {
                if (!autoSaveExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                    autoSaveExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                autoSaveExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
