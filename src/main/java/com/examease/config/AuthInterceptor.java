package com.examease.config;

import com.examease.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        HttpSession session = request.getSession(false);
        User loggedInUser = (session != null) ? (User) session.getAttribute("loggedInUser") : null;

        // Prevent browser caching for dynamic/authenticated pages
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        // Protect student routes
        if (uri.startsWith("/student")) {
            if (loggedInUser == null) {
                response.sendRedirect("/login?unauthorized=true");
                return false;
            }
            if (!"STUDENT".equalsIgnoreCase(loggedInUser.getRole())) {
                response.sendRedirect("/admin/dashboard");
                return false;
            }
        }

        // Protect admin routes (except /admin/login)
        if (uri.startsWith("/admin") && !uri.equals("/admin/login")) {
            if (loggedInUser == null) {
                response.sendRedirect("/admin/login?unauthorized=true");
                return false;
            }
            if (!"ADMIN".equalsIgnoreCase(loggedInUser.getRole())) {
                response.sendRedirect("/student/dashboard");
                return false;
            }
        }

        return true;
    }
}
