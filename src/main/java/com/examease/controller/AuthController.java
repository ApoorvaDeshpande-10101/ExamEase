package com.examease.controller;

import com.examease.exception.InvalidCredentialsException;
import com.examease.exception.UserAlreadyExistsException;
import com.examease.model.User;
import com.examease.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/register")
    public String showRegisterPage(HttpSession session) {
        User loggedIn = (User) session.getAttribute("loggedInUser");
        if (loggedIn != null) {
            return "STUDENT".equals(loggedIn.getRole()) ? "redirect:/student/dashboard" : "redirect:/admin/dashboard";
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String name,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam(name = "confirmPassword", required = false) String confirmPassword,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            authService.register(name, email, password, confirmPassword != null ? confirmPassword : password);
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! You may now log in with your credentials.");
            return "redirect:/login";
        } catch (IllegalArgumentException | UserAlreadyExistsException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            return "register";
        }
    }

    @GetMapping("/login")
    public String showLoginPage(@RequestParam(value = "unauthorized", required = false) String unauthorized,
                                @RequestParam(value = "logout", required = false) String logout,
                                HttpSession session,
                                Model model) {
        User loggedIn = (User) session.getAttribute("loggedInUser");
        if (loggedIn != null) {
            return "STUDENT".equals(loggedIn.getRole()) ? "redirect:/student/dashboard" : "redirect:/admin/dashboard";
        }

        if (unauthorized != null) {
            model.addAttribute("error", "Please log in as a student to access this page.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully.");
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        try {
            User user = authService.authenticateStudent(email, password);
            session.setAttribute("loggedInUser", user);
            return "redirect:/student/dashboard";
        } catch (InvalidCredentialsException | IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("email", email);
            return "login";
        }
    }

    @GetMapping("/admin/login")
    public String showAdminLoginPage(@RequestParam(value = "unauthorized", required = false) String unauthorized,
                                     HttpSession session,
                                     Model model) {
        User loggedIn = (User) session.getAttribute("loggedInUser");
        if (loggedIn != null) {
            return "ADMIN".equals(loggedIn.getRole()) ? "redirect:/admin/dashboard" : "redirect:/student/dashboard";
        }

        if (unauthorized != null) {
            model.addAttribute("error", "Please log in with administrator credentials.");
        }
        return "admin-login";
    }

    @PostMapping("/admin/login")
    public String adminLogin(@RequestParam String email,
                             @RequestParam String password,
                             HttpSession session,
                             Model model) {
        try {
            User admin = authService.authenticateAdmin(email, password);
            session.setAttribute("loggedInUser", admin);
            return "redirect:/admin/dashboard";
        } catch (InvalidCredentialsException | IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("email", email);
            return "admin-login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout=true";
    }
}
