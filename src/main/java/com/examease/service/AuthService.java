package com.examease.service;

import com.examease.exception.InvalidCredentialsException;
import com.examease.exception.UserAlreadyExistsException;
import com.examease.model.User;
import com.examease.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final BackgroundTaskService backgroundTaskService;

    public AuthService(UserRepository userRepository, BackgroundTaskService backgroundTaskService) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.backgroundTaskService = backgroundTaskService;
    }

    public User register(String name, String email, String password, String confirmPassword) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            throw new IllegalArgumentException("A valid email address is required.");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Password and Confirm Password do not match.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new UserAlreadyExistsException("An account with email '" + normalizedEmail + "' is already registered.");
        }

        String hashedPassword = passwordEncoder.encode(password);
        User user = new User(name.trim(), normalizedEmail, hashedPassword, "STUDENT");
        userRepository.save(user);

        // Async background notification
        backgroundTaskService.notifyEvent("STUDENT_REGISTERED", "New student registered: " + user.getName() + " (" + user.getEmail() + ")");

        return user;
    }

    public User authenticateStudent(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new InvalidCredentialsException("Email and password cannot be empty.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password.");
        }

        if (!"STUDENT".equalsIgnoreCase(user.getRole())) {
            throw new InvalidCredentialsException("This account is an Administrator. Please use the Admin Login page.");
        }

        return user;
    }

    public User authenticateAdmin(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new InvalidCredentialsException("Email and password cannot be empty.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid admin email or password."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid admin email or password.");
        }

        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new InvalidCredentialsException("Access denied. This account does not possess Administrator privileges.");
        }

        return user;
    }

    public boolean isEmailRegistered(String email) {
        if (email == null) return false;
        return userRepository.findByEmail(email.trim().toLowerCase()).isPresent();
    }
}
