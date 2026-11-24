package com.examease.controller;

import com.examease.model.User;
import com.examease.service.ResultService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/results")
public class ResultController {

    private final ResultService resultService;

    public ResultController(ResultService resultService) {
        this.resultService = resultService;
    }

    @GetMapping("/{id}")
    public String viewResult(@PathVariable("id") String id,
                             HttpSession session,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        try {
            Map<String, Object> details = resultService.getResultDetails(id, loggedInUser);
            model.addAllAttributes(details);
            model.addAttribute("user", loggedInUser);
            return "result/details";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "STUDENT".equals(loggedInUser.getRole()) ? "redirect:/student/results" : "redirect:/admin/analytics";
        }
    }
}
