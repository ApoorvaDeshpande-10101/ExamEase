package com.examease;

import com.examease.exception.InvalidCredentialsException;
import com.examease.exception.UserAlreadyExistsException;
import com.examease.model.*;
import com.examease.repository.AttemptRepository;
import com.examease.repository.ExamRepository;
import com.examease.repository.ResultRepository;
import com.examease.repository.UserRepository;
import com.examease.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

import com.examease.controller.ExamController;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

@SpringBootTest
class ExameaseApplicationTests {

    @Autowired
    private ExamController examController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private AttemptRepository attemptRepository;

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private ExamService examService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private BackgroundTaskService backgroundTaskService;

    @Test
    @DisplayName("1. Context loads and default admin is initialized")
    void contextLoadsAndDefaultAdminInitialized() {
        Optional<User> adminOpt = userRepository.findByEmail("admin@examease.com");
        assertTrue(adminOpt.isPresent(), "Default administrator should be initialized");
        assertEquals("ADMIN", adminOpt.get().getRole());
    }

    @Test
    @DisplayName("2. Java Object Serialization & Deserialization Test")
    void testJavaObjectSerialization() throws IOException, ClassNotFoundException {
        Question q = new Question("What is Java?", "Language", "Coffee", "Car", "Animal", "A", 1);
        Exam exam = new Exam("Serialization Test Exam", "Testing Java Object Streams", 20, 1, 1, true);
        exam.addQuestion(q);

        // Serialize to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(exam);
        }

        // Deserialize
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        Exam deserializedExam;
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            deserializedExam = (Exam) ois.readObject();
        }

        assertNotNull(deserializedExam);
        assertEquals(exam.getTitle(), deserializedExam.getTitle());
        assertEquals(1, deserializedExam.getQuestions().size());
        assertEquals("What is Java?", deserializedExam.getQuestions().get(0).getQuestionText());
    }

    @Test
    @DisplayName("3. Student Registration and BCrypt Authentication Test")
    void testStudentRegistrationAndAuthentication() {
        String email = "teststudent_" + System.currentTimeMillis() + "@examease.com";
        User student = authService.register("Test Student", email, "Secret@123", "Secret@123");

        assertNotNull(student);
        assertEquals("STUDENT", student.getRole());
        assertNotEquals("Secret@123", student.getPassword(), "Password must be BCrypt hashed, never plain text");

        // Authenticate successfully
        User authenticated = authService.authenticateStudent(email, "Secret@123");
        assertNotNull(authenticated);
        assertEquals(student.getId(), authenticated.getId());

        // Test invalid password
        assertThrows(InvalidCredentialsException.class, () ->
                authService.authenticateStudent(email, "WrongPassword")
        );

        // Test duplicate registration rejection
        assertThrows(UserAlreadyExistsException.class, () ->
                authService.register("Duplicate", email, "Secret@123", "Secret@123")
        );
    }

    @Test
    @DisplayName("4. Examination Creation, Question Management & Auto-Calculation Test")
    void testExamAndQuestionManagement() {
        Exam exam = examService.createExam("Unit Test Exam " + System.currentTimeMillis(), "Testing description", 25, 2);
        assertNotNull(exam);
        assertFalse(exam.isPublished());
        assertEquals(0, exam.getTotalMarks());

        // Add 2 questions (1 mark and 2 marks)
        examService.addQuestionToExam(exam.getId(), "Q1 Question Text?", "A", "B", "C", "D", "A", 1);
        examService.addQuestionToExam(exam.getId(), "Q2 Question Text?", "A", "B", "C", "D", "B", 2);

        Exam updated = examService.getExamById(exam.getId());
        assertEquals(2, updated.getQuestions().size());
        assertEquals(3, updated.getTotalMarks(), "Total marks must sum to 3 (1 + 2)");

        // Toggle publish
        examService.togglePublishStatus(exam.getId());
        Exam published = examService.getExamById(exam.getId());
        assertTrue(published.isPublished());
    }

    @Test
    @DisplayName("5. Examination Taking, Server-Side Evaluation and Scoring Test")
    void testExamTakingAndScoring() {
        // Create student
        String email = "taker_" + System.currentTimeMillis() + "@test.com";
        User student = authService.register("Exam Taker", email, "Password@123", "Password@123");

        // Create published exam
        Exam exam = examService.createExam("Scoring Test Exam", "Testing evaluation", 10, 1);
        Question q1 = examService.addQuestionToExam(exam.getId(), "Is Java object-oriented?", "Yes", "No", "Maybe", "Never", "A", 1);
        Question q2 = examService.addQuestionToExam(exam.getId(), "Is 2+2=4?", "Yes", "No", "Maybe", "Never", "A", 1);
        examService.togglePublishStatus(exam.getId());

        // Start exam
        ExamAttempt attempt = studentService.startOrResumeExam(student.getId(), exam.getId());
        assertNotNull(attempt);
        assertFalse(attempt.isSubmitted());

        // Save answers: Q1 = A (Correct), Q2 = B (Incorrect)
        attempt.recordAnswer(q1.getId(), "A");
        attempt.recordAnswer(q2.getId(), "B");
        attemptRepository.save(attempt);

        // Submit exam
        Result result = studentService.submitExam(attempt.getId(), student.getId());
        assertNotNull(result);
        assertEquals(2, result.getTotalQuestions());
        assertEquals(1, result.getCorrectAnswers());
        assertEquals(1, result.getIncorrectAnswers());
        assertEquals(0, result.getUnansweredQuestions());
        assertEquals(1, result.getMarksObtained());
        assertEquals(2, result.getMaximumMarks());
        assertEquals(50.0, result.getPercentage());
        assertEquals("PASS", result.getStatus(), "1 mark obtained >= passing marks 1 => PASS");
    }

    @Test
    @DisplayName("6. Java Streams API Analytics Verification")
    void testStreamsAnalytics() {
        // Register a test student for streams searching
        String uniqueName = "StreamsStudent_" + System.currentTimeMillis();
        authService.register(uniqueName, uniqueName.toLowerCase() + "@streams.com", "Password@123", "Password@123");

        // Search students using Streams filter
        List<User> found = analyticsService.searchStudents(uniqueName);
        assertFalse(found.isEmpty(), "Streams search should locate the registered student");
        assertEquals(uniqueName, found.get(0).getName());

        // Filter exams
        List<Exam> publishedExams = analyticsService.filterExams(true, null);
        assertTrue(publishedExams.stream().allMatch(Exam::isPublished));

        // Overall stats
        Map<String, Object> stats = analyticsService.getOverallPerformanceStats();
        assertNotNull(stats);
        assertTrue(stats.containsKey("totalStudents"));
        assertTrue(stats.containsKey("totalExams"));
        assertTrue(stats.containsKey("totalAttempts"));
        assertTrue(stats.containsKey("passRate"));

        // Exam wise breakdown
        List<Map<String, Object>> breakdown = analyticsService.getExamWisePerformanceReports();
        assertNotNull(breakdown);
    }

    @Test
    @DisplayName("7. Multithreading Background Auto-Submit on Deadline Expiration Test")
    void testBackgroundDeadlineExpiration() {
        User student = authService.register("Expired Student " + System.currentTimeMillis(),
                "exp_" + System.currentTimeMillis() + "@test.com", "Password@123", "Password@123");

        Exam exam = examService.createExam("Expired Exam " + System.currentTimeMillis(), "Test", 1, 1);
        examService.addQuestionToExam(exam.getId(), "Quick Q?", "A", "B", "C", "D", "A", 1);
        examService.togglePublishStatus(exam.getId());

        // Create an attempt whose deadline is already 1 minute in the past
        ExamAttempt pastAttempt = new ExamAttempt(student.getId(), exam.getId(),
                LocalDateTime.now().minusMinutes(2),
                LocalDateTime.now().minusMinutes(1));
        attemptRepository.save(pastAttempt);

        // Run background expiration check
        backgroundTaskService.checkAndAutoSubmitExpiredAttempts();

        // Verify the attempt was automatically marked submitted
        ExamAttempt verified = attemptRepository.findById(pastAttempt.getId()).orElse(null);
        assertNotNull(verified);
        assertTrue(verified.isSubmitted(), "Expired attempt must be automatically submitted by background task");

        // Verify result was recorded
        Optional<Result> resOpt = resultRepository.findLatestByStudentIdAndExamId(student.getId(), exam.getId());
        assertTrue(resOpt.isPresent(), "Result must be generated on background auto-submit");
    }

    @Test
    @DisplayName("8. Test Admin Create Exam View and Submission Controller Methods")
    void testAdminCreateExamWorkflow() {
        User admin = userRepository.findByEmail("admin@examease.com").orElseThrow();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUser", admin);
        Model model = new ConcurrentModel();

        // 1. showCreateForm must return "admin/exam-form", and populate exam & isEdit = false
        String view = examController.showCreateForm(session, model);
        assertEquals("admin/exam-form", view);
        assertTrue(model.containsAttribute("exam"));
        assertTrue(model.containsAttribute("isEdit"));
        assertEquals(Boolean.FALSE, model.getAttribute("isEdit"));

        // 2. createExam with valid params
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String redirect = examController.createExam("Controller Created Exam", "Description", 20, 2, redirectAttributes);
        assertTrue(redirect.startsWith("redirect:/admin/exams/"));
        assertTrue(redirect.endsWith("/questions"));
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("successMessage"));

        // 3. createExam with empty description should also succeed
        RedirectAttributesModelMap redirectAttributes2 = new RedirectAttributesModelMap();
        String redirect2 = examController.createExam("Empty Desc Exam", "", 15, 1, redirectAttributes2);
        assertTrue(redirect2.startsWith("redirect:/admin/exams/"));
        assertTrue(redirect2.endsWith("/questions"));
        assertTrue(redirectAttributes2.getFlashAttributes().containsKey("successMessage"));
    }
}
