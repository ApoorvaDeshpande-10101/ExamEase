
package com.examease.controller;

import com.examease.exception.ExamAlreadySubmittedException;
import com.examease.exception.ExamNotFoundException;
import com.examease.exception.UnauthorizedException;
import com.examease.model.*;
import com.examease.service.ExamService;
import com.examease.service.ResultService;
import com.examease.service.StudentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final StudentService studentService;
    private final ExamService examService;
    private final ResultService resultService;

    public StudentController(StudentService studentService,
                             ExamService examService,
                             ResultService resultService) {
        this.studentService = studentService;
        this.examService = examService;
        this.resultService = resultService;
    }

    private User getLoggedInStudent(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User student = getLoggedInStudent(session);

        Map<String, Object> stats =
                studentService.getStudentDashboardStats(student.getId());

        // Show only published exams accessible to this student
        List<Exam> publishedExams = examService.getPublishedExams()
                .stream()
                .filter(exam -> exam.isAccessible(student.getId()))
                .toList();

        List<Result> recentResults =
                studentService.getStudentRecentResults(student.getId(), 5);

        // Precompute which exams the student has already taken
        Set<String> attemptedExamIds = new HashSet<>();

        for (Result r : resultService.getResultsByStudentId(student.getId())) {
            attemptedExamIds.add(r.getExamId());
        }

        // Map exam titles for recent results table
        Map<String, String> examTitles = new HashMap<>();

        for (Exam e : examService.getAllExams()) {
            examTitles.put(e.getId(), e.getTitle());
        }

        model.addAttribute("student", student);
        model.addAttribute("stats", stats);
        model.addAttribute("availableExams", publishedExams);
        model.addAttribute("attemptedExamIds", attemptedExamIds);
        model.addAttribute("recentResults", recentResults);
        model.addAttribute("examTitles", examTitles);

        return "student/dashboard";
    }

    @GetMapping("/exams")
    public String availableExams(HttpSession session, Model model) {
        User student = getLoggedInStudent(session);

        // Show only exams assigned to this student
        List<Exam> publishedExams = examService.getPublishedExams()
                .stream()
                .filter(exam -> exam.isAccessible(student.getId()))
                .toList();

        Set<String> attemptedExamIds = new HashSet<>();

        for (Result r : resultService.getResultsByStudentId(student.getId())) {
            attemptedExamIds.add(r.getExamId());
        }

        model.addAttribute("student", student);
        model.addAttribute("exams", publishedExams);
        model.addAttribute("attemptedExamIds", attemptedExamIds);

        return "student/exams";
    }

    @GetMapping("/exams/{id}/instructions")
    public String examInstructions(@PathVariable("id") String examId,
                                   HttpSession session,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        User student = getLoggedInStudent(session);
        Exam exam = examService.getExamById(examId);

        if (!exam.isPublished()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "This examination is not published yet."
            );
            return "redirect:/student/exams";
        }

        // Prevent students from opening instructions for unassigned exams
        if (!exam.isAccessible(student.getId())) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "You are not assigned to this examination."
            );
            return "redirect:/student/exams";
        }

        if (studentService.hasStudentAttempted(student.getId(), examId)) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "You have already completed this examination."
            );
            return "redirect:/student/results";
        }

        model.addAttribute("student", student);
        model.addAttribute("exam", exam);

        return "exam/instructions";
    }

    @GetMapping("/exams/{id}/start")
    public String startExam(@PathVariable("id") String examId,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        User student = getLoggedInStudent(session);

        try {
            ExamAttempt attempt =
                    studentService.startOrResumeExam(student.getId(), examId);

            return "redirect:/student/exam/take/" + attempt.getId();

        } catch (ExamAlreadySubmittedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/student/results";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/student/exams";
        }
    }

    @GetMapping("/exam/take/{attemptId}")
    public String takeExam(@PathVariable("attemptId") String attemptId,
                           HttpSession session,
                           Model model,
                           RedirectAttributes redirectAttributes) {

        User student = getLoggedInStudent(session);
        ExamAttempt attempt;

        try {
            attempt = studentService.getAttempt(attemptId, student.getId());

        } catch (UnauthorizedException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/student/dashboard";
        }

        if (attempt.isSubmitted()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "This examination has already been submitted."
            );
            return "redirect:/student/results";
        }

        Exam exam = examService.getExamById(attempt.getExamId());

        // Verify access again before displaying the examination
        if (!exam.isPublished() || !exam.isAccessible(student.getId())) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "You are not allowed to access this examination."
            );
            return "redirect:/student/exams";
        }

        // Check if deadline passed
        long remainingSeconds =
                Duration.between(LocalDateTime.now(), attempt.getDeadline()).toSeconds();

        if (remainingSeconds <= 0) {
            Result result = studentService.submitExam(attemptId, student.getId());
            return "redirect:/student/results/" + result.getId();
        }

        // Create student-safe questions
        // This prevents correct answers from being exposed in HTML / DOM
        List<Map<String, Object>> safeQuestions = new ArrayList<>();

        if (exam.getQuestions() != null) {
            int qIndex = 1;

            for (Question q : exam.getQuestions()) {
                Map<String, Object> sq = new HashMap<>();

                sq.put("index", qIndex++);
                sq.put("id", q.getId());
                sq.put("questionText", q.getQuestionText());
                sq.put("optionA", q.getOptionA());
                sq.put("optionB", q.getOptionB());
                sq.put("optionC", q.getOptionC());
                sq.put("optionD", q.getOptionD());
                sq.put("marks", q.getMarks());

                safeQuestions.add(sq);
            }
        }

        model.addAttribute("student", student);
        model.addAttribute("exam", exam);
        model.addAttribute("attempt", attempt);
        model.addAttribute("questions", safeQuestions);
        model.addAttribute("totalQuestions", safeQuestions.size());
        model.addAttribute("remainingSeconds", remainingSeconds);
        model.addAttribute("selectedAnswers", attempt.getSelectedAnswers());
        model.addAttribute("markedForReview", attempt.getMarkedForReview());

        return "exam/take";
    }

    @PostMapping("/exam/save-answer")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveAnswer(
            @RequestBody Map<String, Object> payload,
            HttpSession session) {

        User student = getLoggedInStudent(session);

        if (student == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "Unauthorized"));
        }

        String attemptId = (String) payload.get("attemptId");
        String questionId = (String) payload.get("questionId");
        String answer = (String) payload.get("answer");
        Boolean markForReview = (Boolean) payload.get("markForReview");

        try {
            studentService.saveAnswer(
                    attemptId,
                    student.getId(),
                    questionId,
                    answer,
                    markForReview
            );

            return ResponseEntity.ok(Map.of("success", true));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/exam/submit")
    public String submitExam(@RequestParam("attemptId") String attemptId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {

        User student = getLoggedInStudent(session);

        try {
            Result result =
                    studentService.submitExam(attemptId, student.getId());

            return "redirect:/student/results/" + result.getId();

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/student/results";
        }
    }

    @GetMapping("/results")
    public String resultsHistory(HttpSession session, Model model) {
        User student = getLoggedInStudent(session);

        List<Result> results =
                resultService.getResultsByStudentId(student.getId());

        Map<String, String> examTitles = new HashMap<>();

        for (Exam e : examService.getAllExams()) {
            examTitles.put(e.getId(), e.getTitle());
        }

        model.addAttribute("student", student);
        model.addAttribute("results", results);
        model.addAttribute("examTitles", examTitles);

        return "student/results";
    }

    @GetMapping("/results/{id}")
    public String viewResultDetails(@PathVariable("id") String resultId,
                                    HttpSession session,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {

        User student = getLoggedInStudent(session);

        try {
            Map<String, Object> details =
                    resultService.getResultDetails(resultId, student);

            model.addAllAttributes(details);
            model.addAttribute("student", student);

            return "result/details";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/student/results";
        }
    }
}
