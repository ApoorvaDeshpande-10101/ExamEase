
package com.examease.controller;

import com.examease.model.*;
import com.examease.service.AnalyticsService;
import com.examease.service.ExamService;
import com.examease.service.ResultService;
import com.examease.service.StudentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AnalyticsService analyticsService;
    private final ExamService examService;
    private final StudentService studentService;
    private final ResultService resultService;

    public AdminController(AnalyticsService analyticsService,
                           ExamService examService,
                           StudentService studentService,
                           ResultService resultService) {
        this.analyticsService = analyticsService;
        this.examService = examService;
        this.studentService = studentService;
        this.resultService = resultService;
    }

    private User getLoggedInAdmin(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }

    private boolean isAdmin(User user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User admin = getLoggedInAdmin(session);

        Map<String, Object> stats = analyticsService.getOverallPerformanceStats();
        List<Exam> recentExams = examService.getAllExams();
        List<Notification> notifications = analyticsService.getRecentNotifications(10);
        List<Result> recentResults = resultService.getAllResults();

        // Student & exam name lookup.
        Map<String, String> studentNames = new HashMap<>();

        for (User s : studentService.getAllStudents()) {
            studentNames.put(s.getId(), s.getName());
        }

        Map<String, String> examTitles = new HashMap<>();

        for (Exam e : recentExams) {
            examTitles.put(e.getId(), e.getTitle());
        }

        model.addAttribute("admin", admin);
        model.addAttribute("stats", stats);
        model.addAttribute("recentExams", recentExams);
        model.addAttribute("notifications", notifications);
        model.addAttribute("recentResults", recentResults.stream().limit(8).toList());
        model.addAttribute("studentNames", studentNames);
        model.addAttribute("examTitles", examTitles);

        return "admin/dashboard";
    }

    @GetMapping("/students")
    public String students(@RequestParam(name = "query", required = false) String query,
                           HttpSession session,
                           Model model) {

        User admin = getLoggedInAdmin(session);
        List<User> students = analyticsService.searchStudents(query);

        // Calculate student attempt count and average score per student using Streams.
        Map<String, Integer> studentAttemptCounts = new HashMap<>();
        Map<String, Double> studentAvgScores = new HashMap<>();

        for (User s : students) {
            List<Result> studentResults = resultService.getResultsByStudentId(s.getId());

            studentAttemptCounts.put(s.getId(), studentResults.size());

            double avg = studentResults.stream()
                    .mapToDouble(Result::getPercentage)
                    .average()
                    .orElse(0.0);

            studentAvgScores.put(s.getId(), Math.round(avg * 10.0) / 10.0);
        }

        model.addAttribute("admin", admin);
        model.addAttribute("students", students);
        model.addAttribute("query", query);
        model.addAttribute("attemptCounts", studentAttemptCounts);
        model.addAttribute("avgScores", studentAvgScores);

        return "admin/students";
    }

    @GetMapping("/students/{id}/results")
    public String studentResults(@PathVariable("id") String studentId,
                                 HttpSession session,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {

        User admin = getLoggedInAdmin(session);

        try {
            User student = studentService.getStudentById(studentId);
            List<Result> results = resultService.getResultsByStudentId(studentId);

            Map<String, String> examTitles = new HashMap<>();

            for (Exam e : examService.getAllExams()) {
                examTitles.put(e.getId(), e.getTitle());
            }

            model.addAttribute("admin", admin);
            model.addAttribute("student", student);
            model.addAttribute("results", results);
            model.addAttribute("examTitles", examTitles);

            return "admin/student-results";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/students";
        }
    }

    /**
     * Deletes a student account and the student's related examination data.
     * This operation is available only to logged-in administrators.
     */
    @PostMapping("/students/{id}/delete")
    public String deleteStudent(@PathVariable("id") String studentId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {

        User admin = getLoggedInAdmin(session);

        // Server-side authorization check.
        if (!isAdmin(admin)) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Access denied. Only administrators can delete student accounts."
            );

            return "redirect:/login";
        }

        // Prevent an account from deleting itself.
        if (Objects.equals(admin.getId(), studentId)) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "You cannot delete your own account."
            );

            return "redirect:/admin/students";
        }

        try {
            studentService.deleteStudent(studentId);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Student account and associated examination records deleted successfully."
            );

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage() != null
                            ? e.getMessage()
                            : "Unable to delete the student account."
            );
        }

        return "redirect:/admin/students";
    }

    @GetMapping("/analytics")
    public String analytics(HttpSession session, Model model) {
        User admin = getLoggedInAdmin(session);

        Map<String, Object> stats = analyticsService.getOverallPerformanceStats();
        List<Map<String, Object>> examReports =
                analyticsService.getExamWisePerformanceReports();

        List<Result> topScoringResults = analyticsService
                .sortResultsByScore(true)
                .stream()
                .limit(10)
                .toList();

        Map<String, String> studentNames = new HashMap<>();

        for (User s : studentService.getAllStudents()) {
            studentNames.put(s.getId(), s.getName());
        }

        Map<String, String> examTitles = new HashMap<>();

        for (Exam e : examService.getAllExams()) {
            examTitles.put(e.getId(), e.getTitle());
        }

        model.addAttribute("admin", admin);
        model.addAttribute("stats", stats);
        model.addAttribute("examReports", examReports);
        model.addAttribute("topResults", topScoringResults);
        model.addAttribute("studentNames", studentNames);
        model.addAttribute("examTitles", examTitles);

        return "admin/analytics";
    }

    @GetMapping("/results/{id}")
    public String viewAdminResult(@PathVariable("id") String resultId,
                                  HttpSession session,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {

        User admin = getLoggedInAdmin(session);

        try {
            Map<String, Object> details = resultService.getResultDetails(resultId, admin);

            model.addAllAttributes(details);
            model.addAttribute("admin", admin);

            return "result/details";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/analytics";
        }
    }
}
