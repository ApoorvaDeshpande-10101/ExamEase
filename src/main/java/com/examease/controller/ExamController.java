
package com.examease.controller;

import com.examease.exception.InvalidExamDataException;
import com.examease.model.Exam;
import com.examease.model.Question;
import com.examease.model.User;
import com.examease.service.ExamService;
import com.examease.service.StudentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/exams")
public class ExamController {

    private final ExamService examService;
    private final StudentService studentService;

    public ExamController(ExamService examService,
                          StudentService studentService) {
        this.examService = examService;
        this.studentService = studentService;
    }

    private User getLoggedInAdmin(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }

    @GetMapping
    public String listExams(HttpSession session, Model model) {
        User admin = getLoggedInAdmin(session);
        List<Exam> exams = examService.getAllExams();

        model.addAttribute("admin", admin);
        model.addAttribute("exams", exams);

        return "admin/exams";
    }

    @GetMapping("/create")
    public String showCreateForm(HttpSession session, Model model) {
        User admin = getLoggedInAdmin(session);

        model.addAttribute("admin", admin);
        model.addAttribute("exam", new Exam());
        model.addAttribute("students", studentService.getAllStudents());
        model.addAttribute("isEdit", false);

        return "admin/exam-form";
    }

    // Backward-compatible method used by existing tests.
    // Defaults to ALL students, matching the normal form default.
    public String createExam(
            String title,
            String description,
            int duration,
            int passingMarks,
            RedirectAttributes redirectAttributes) {

        return createExam(
                title,
                description,
                duration,
                passingMarks,
                "ALL",
                null,
                redirectAttributes
        );
    }

    @PostMapping("/create")
    public String createExam(
            @RequestParam String title,
            @RequestParam(name = "description", required = false, defaultValue = "") String description,
            @RequestParam(name = "duration", required = false, defaultValue = "15") int duration,
            @RequestParam(name = "passingMarks", required = false, defaultValue = "1") int passingMarks,
            @RequestParam(name = "accessType", defaultValue = "ALL") String accessType,
            @RequestParam(name = "allowedStudentIds", required = false) List<String> allowedStudentIds,
            RedirectAttributes redirectAttributes) {

        try {
            Exam exam = examService.createExam(
                    title,
                    description,
                    duration,
                    passingMarks,
                    accessType,
                    allowedStudentIds
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Examination '" + exam.getTitle()
                            + "' created successfully. You can now add questions."
            );

            return "redirect:/admin/exams/" + exam.getId() + "/questions";

        } catch (InvalidExamDataException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/exams/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(
            @PathVariable("id") String id,
            HttpSession session,
            Model model) {

        User admin = getLoggedInAdmin(session);
        Exam exam = examService.getExamById(id);

        model.addAttribute("admin", admin);
        model.addAttribute("exam", exam);
        model.addAttribute("students", studentService.getAllStudents());
        model.addAttribute("isEdit", true);

        return "admin/exam-form";
    }

    @PostMapping("/{id}/edit")
    public String updateExam(
            @PathVariable("id") String id,
            @RequestParam String title,
            @RequestParam(name = "description", required = false, defaultValue = "") String description,
            @RequestParam(name = "duration", required = false, defaultValue = "15") int duration,
            @RequestParam(name = "passingMarks", required = false, defaultValue = "1") int passingMarks,
            @RequestParam(name = "accessType", defaultValue = "ALL") String accessType,
            @RequestParam(name = "allowedStudentIds", required = false) List<String> allowedStudentIds,
            RedirectAttributes redirectAttributes) {

        try {
            examService.updateExam(
                    id,
                    title,
                    description,
                    duration,
                    passingMarks,
                    accessType,
                    allowedStudentIds
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Examination details updated successfully."
            );

            return "redirect:/admin/exams";

        } catch (InvalidExamDataException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/exams/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteExam(
            @PathVariable("id") String id,
            RedirectAttributes redirectAttributes) {

        try {
            examService.deleteExam(id);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Examination deleted successfully."
            );

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/exams";
    }

    @PostMapping("/{id}/toggle-publish")
    public String togglePublish(
            @PathVariable("id") String id,
            RedirectAttributes redirectAttributes) {

        try {
            Exam exam = examService.togglePublishStatus(id);
            String status = exam.isPublished() ? "published" : "unpublished";

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Examination " + status + " successfully."
            );

        } catch (InvalidExamDataException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/exams";
    }

    // Question Management

    @GetMapping("/{id}/questions")
    public String manageQuestions(
            @PathVariable("id") String examId,
            HttpSession session,
            Model model) {

        User admin = getLoggedInAdmin(session);
        Exam exam = examService.getExamById(examId);

        model.addAttribute("admin", admin);
        model.addAttribute("exam", exam);
        model.addAttribute("questions", exam.getQuestions());

        return "admin/questions";
    }

    @GetMapping("/{id}/questions/add")
    public String showAddQuestionForm(
            @PathVariable("id") String examId,
            HttpSession session,
            Model model) {

        User admin = getLoggedInAdmin(session);
        Exam exam = examService.getExamById(examId);

        model.addAttribute("admin", admin);
        model.addAttribute("exam", exam);
        model.addAttribute("question", new Question());
        model.addAttribute("isEdit", false);

        return "admin/question-form";
    }

    @PostMapping("/{id}/questions/add")
    public String addQuestion(
            @PathVariable("id") String examId,
            @RequestParam String questionText,
            @RequestParam String optionA,
            @RequestParam String optionB,
            @RequestParam String optionC,
            @RequestParam String optionD,
            @RequestParam String correctAnswer,
            @RequestParam int marks,
            RedirectAttributes redirectAttributes) {

        try {
            examService.addQuestionToExam(
                    examId, questionText, optionA, optionB,
                    optionC, optionD, correctAnswer, marks
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Question added successfully."
            );

            return "redirect:/admin/exams/" + examId + "/questions";

        } catch (InvalidExamDataException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/exams/" + examId + "/questions/add";
        }
    }

    @GetMapping("/{id}/questions/{qId}/edit")
    public String showEditQuestionForm(
            @PathVariable("id") String examId,
            @PathVariable("qId") String qId,
            HttpSession session,
            Model model) {

        User admin = getLoggedInAdmin(session);
        Exam exam = examService.getExamById(examId);

        Question target = exam.getQuestions().stream()
                .filter(q -> q.getId().equals(qId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));

        model.addAttribute("admin", admin);
        model.addAttribute("exam", exam);
        model.addAttribute("question", target);
        model.addAttribute("isEdit", true);

        return "admin/question-form";
    }

    @PostMapping("/{id}/questions/{qId}/edit")
    public String updateQuestion(
            @PathVariable("id") String examId,
            @PathVariable("qId") String qId,
            @RequestParam String questionText,
            @RequestParam String optionA,
            @RequestParam String optionB,
            @RequestParam String optionC,
            @RequestParam String optionD,
            @RequestParam String correctAnswer,
            @RequestParam int marks,
            RedirectAttributes redirectAttributes) {

        try {
            examService.updateQuestion(
                    examId, qId, questionText, optionA, optionB,
                    optionC, optionD, correctAnswer, marks
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Question updated successfully."
            );

            return "redirect:/admin/exams/" + examId + "/questions";

        } catch (InvalidExamDataException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());

            return "redirect:/admin/exams/" + examId
                    + "/questions/" + qId + "/edit";
        }
    }

    @PostMapping("/{id}/questions/{qId}/delete")
    public String deleteQuestion(
            @PathVariable("id") String examId,
            @PathVariable("qId") String qId,
            RedirectAttributes redirectAttributes) {

        try {
            examService.deleteQuestion(examId, qId);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Question removed successfully."
            );

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/exams/" + examId + "/questions";
    }
}
