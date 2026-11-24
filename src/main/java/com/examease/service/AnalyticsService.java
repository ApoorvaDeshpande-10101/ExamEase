package com.examease.service;

import com.examease.model.Exam;
import com.examease.model.Notification;
import com.examease.model.Result;
import com.examease.model.User;
import com.examease.repository.ExamRepository;
import com.examease.repository.NotificationRepository;
import com.examease.repository.ResultRepository;
import com.examease.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service demonstrating advanced Java Streams API operations:
 * filter, map, sorted, collect, count, average, max, groupingBy
 */
@Service
public class AnalyticsService {

    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final ResultRepository resultRepository;
    private final NotificationRepository notificationRepository;

    public AnalyticsService(UserRepository userRepository,
                            ExamRepository examRepository,
                            ResultRepository resultRepository,
                            NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.examRepository = examRepository;
        this.resultRepository = resultRepository;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Streams: filter(), collect()
     * Filtering examinations by publish status and title/keyword
     */
    public List<Exam> filterExams(Boolean publishedOnly, String keyword) {
        return examRepository.findAll().stream()
                .filter(exam -> publishedOnly == null || exam.isPublished() == publishedOnly)
                .filter(exam -> keyword == null || keyword.isBlank() ||
                        exam.getTitle().toLowerCase().contains(keyword.toLowerCase()) ||
                        exam.getDescription().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Streams: filter(), map(), collect()
     * Searching students by name or email keyword
     */
    public List<User> searchStudents(String query) {
        return userRepository.findAll().stream()
                .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole()))
                .filter(u -> query == null || query.isBlank() ||
                        u.getName().toLowerCase().contains(query.toLowerCase()) ||
                        u.getEmail().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Streams: mapToDouble(), average()
     * Calculating average percentage across all examination attempts
     */
    public double calculateAverageMarksOverall() {
        return resultRepository.findAll().stream()
                .mapToDouble(Result::getPercentage)
                .average()
                .orElse(0.0);
    }

    /**
     * Streams: mapToDouble(), max()
     * Finding highest student score percentage
     */
    public double findHighestStudentScore() {
        return resultRepository.findAll().stream()
                .mapToDouble(Result::getPercentage)
                .max()
                .orElse(0.0);
    }

    /**
     * Streams: filter(), count()
     * Counting passed attempts
     */
    public long countPassedStudents() {
        return resultRepository.findAll().stream()
                .filter(r -> "PASS".equalsIgnoreCase(r.getStatus()))
                .count();
    }

    /**
     * Streams: filter(), count()
     * Counting failed attempts
     */
    public long countFailedStudents() {
        return resultRepository.findAll().stream()
                .filter(r -> "FAIL".equalsIgnoreCase(r.getStatus()))
                .count();
    }

    /**
     * Streams: sorted(), collect()
     * Sorting results by percentage or submission date
     */
    public List<Result> sortResultsByScore(boolean descending) {
        Comparator<Result> comparator = Comparator.comparing(Result::getPercentage);
        if (descending) {
            comparator = comparator.reversed();
        }
        return resultRepository.findAll().stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    /**
     * Streams: collect(groupingBy(..., counting())), max(Map.Entry.comparingByValue())
     * Finding the most attempted examination
     */
    public Optional<Exam> findMostAttemptedExam() {
        List<Result> results = resultRepository.findAll();
        if (results.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Long> attemptsPerExam = results.stream()
                .collect(Collectors.groupingBy(Result::getExamId, Collectors.counting()));

        return attemptsPerExam.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .flatMap(entry -> examRepository.findById(entry.getKey()));
    }

    /**
     * Streams: overall statistics aggregation
     */
    public Map<String, Object> getOverallPerformanceStats() {
        List<Result> results = resultRepository.findAll();
        long totalStudents = userRepository.findAll().stream()
                .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole()))
                .count();
        int totalExams = examRepository.findAll().size();
        int totalAttempts = results.size();

        double avgPercentage = results.stream()
                .mapToDouble(Result::getPercentage)
                .average()
                .orElse(0.0);

        double highestPercentage = results.stream()
                .mapToDouble(Result::getPercentage)
                .max()
                .orElse(0.0);

        long passedCount = results.stream()
                .filter(r -> "PASS".equalsIgnoreCase(r.getStatus()))
                .count();

        long failedCount = results.stream()
                .filter(r -> "FAIL".equalsIgnoreCase(r.getStatus()))
                .count();

        double passRate = totalAttempts > 0 ? ((double) passedCount / totalAttempts) * 100.0 : 0.0;

        Optional<Exam> mostAttempted = findMostAttemptedExam();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStudents", totalStudents);
        stats.put("totalExams", totalExams);
        stats.put("totalAttempts", totalAttempts);
        stats.put("averageScore", Math.round(avgPercentage * 100.0) / 100.0);
        stats.put("highestScore", Math.round(highestPercentage * 100.0) / 100.0);
        stats.put("passedCount", passedCount);
        stats.put("failedCount", failedCount);
        stats.put("passRate", Math.round(passRate * 10.0) / 10.0);
        stats.put("mostAttemptedExam", mostAttempted.map(Exam::getTitle).orElse("N/A"));

        return stats;
    }

    /**
     * Streams: exam-wise performance analysis using groupingBy()
     */
    public List<Map<String, Object>> getExamWisePerformanceReports() {
        List<Exam> exams = examRepository.findAll();
        List<Result> allResults = resultRepository.findAll();

        Map<String, List<Result>> resultsByExam = allResults.stream()
                .collect(Collectors.groupingBy(Result::getExamId));

        return exams.stream().map(exam -> {
            List<Result> examResults = resultsByExam.getOrDefault(exam.getId(), Collections.emptyList());
            int attempts = examResults.size();
            long passed = examResults.stream().filter(r -> "PASS".equalsIgnoreCase(r.getStatus())).count();
            long failed = examResults.stream().filter(r -> "FAIL".equalsIgnoreCase(r.getStatus())).count();
            double avgScore = examResults.stream().mapToDouble(Result::getPercentage).average().orElse(0.0);
            double maxScore = examResults.stream().mapToDouble(Result::getPercentage).max().orElse(0.0);
            double passRate = attempts > 0 ? ((double) passed / attempts) * 100.0 : 0.0;

            Map<String, Object> report = new HashMap<>();
            report.put("examId", exam.getId());
            report.put("title", exam.getTitle());
            report.put("duration", exam.getDuration());
            report.put("totalQuestions", exam.getQuestions() != null ? exam.getQuestions().size() : 0);
            report.put("passingMarks", exam.getPassingMarks());
            report.put("totalMarks", exam.getTotalMarks());
            report.put("published", exam.isPublished());
            report.put("attempts", attempts);
            report.put("passed", passed);
            report.put("failed", failed);
            report.put("averageScore", Math.round(avgScore * 10.0) / 10.0);
            report.put("highestScore", Math.round(maxScore * 10.0) / 10.0);
            report.put("passRate", Math.round(passRate * 10.0) / 10.0);
            return report;
        }).collect(Collectors.toList());
    }

    public List<Notification> getRecentNotifications(int limit) {
        return notificationRepository.findAll().stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
}
