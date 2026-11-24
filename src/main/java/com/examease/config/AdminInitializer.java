package com.examease.config;

import com.examease.model.Exam;
import com.examease.model.Question;
import com.examease.model.User;
import com.examease.repository.ExamRepository;
import com.examease.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminInitializer(UserRepository userRepository, ExamRepository examRepository) {
        this.userRepository = userRepository;
        this.examRepository = examRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        initializeDefaultAdmin();
        initializeSampleExams();
    }

    private void initializeDefaultAdmin() {
        String adminEmail = "admin@examease.com";
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = new User(
                    "Administrator",
                    adminEmail,
                    passwordEncoder.encode("Admin@123"),
                    "ADMIN"
            );
            userRepository.save(admin);
            logger.info("Initialized default administrator: {} / Admin@123", adminEmail);
        } else {
            logger.info("Administrator account already exists.");
        }
    }

    private void initializeSampleExams() {
        List<Exam> existingExams = examRepository.findAll();
        if (existingExams.isEmpty()) {
            logger.info("No existing exams found. Initializing sample demonstration examinations...");

            // 1. Java Programming Fundamentals
            Exam javaExam = new Exam(
                    "Java Programming Fundamentals",
                    "Comprehensive assessment of core Java, OOP concepts, Collections framework, Exception Handling, Streams API and Multithreading.",
                    15,
                    5,
                    3,
                    true
            );

            javaExam.addQuestion(new Question(
                    "Which of the following is NOT a valid access modifier in Java?",
                    "public",
                    "protected",
                    "internal",
                    "private",
                    "C",
                    1
            ));

            javaExam.addQuestion(new Question(
                    "Which collection interface in Java maintains insertion order and permits duplicate elements?",
                    "Set",
                    "List",
                    "Map",
                    "Queue",
                    "B",
                    1
            ));

            javaExam.addQuestion(new Question(
                    "Which exception is thrown when an application attempts to access an object reference that is null?",
                    "NullPointerException",
                    "ClassCastException",
                    "ArrayIndexOutOfBoundsException",
                    "IllegalArgumentException",
                    "A",
                    1
            ));

            javaExam.addQuestion(new Question(
                    "In the Java Streams API, which of the following is an intermediate operation?",
                    "collect()",
                    "forEach()",
                    "filter()",
                    "count()",
                    "C",
                    1
            ));

            javaExam.addQuestion(new Question(
                    "Which marker interface must a Java class implement to enable object serialization via ObjectOutputStream?",
                    "Cloneable",
                    "Serializable",
                    "Comparable",
                    "AutoCloseable",
                    "B",
                    1
            ));

            javaExam.recalculateTotalMarks();
            examRepository.save(javaExam);

            // 2. Computer Networks Basics
            Exam netExam = new Exam(
                    "Computer Networks Basics",
                    "Assess fundamental knowledge of network protocols, OSI/TCP-IP reference models, IP addressing, and transmission control.",
                    15,
                    5,
                    3,
                    true
            );

            netExam.addQuestion(new Question(
                    "Which layer of the OSI 7-layer reference model is responsible for logical routing and IP packet delivery?",
                    "Data Link Layer",
                    "Network Layer",
                    "Transport Layer",
                    "Session Layer",
                    "B",
                    1
            ));

            netExam.addQuestion(new Question(
                    "Which transport layer protocol provides connection-oriented, sequenced, and reliable byte-stream delivery?",
                    "UDP",
                    "ICMP",
                    "TCP",
                    "IGMP",
                    "C",
                    1
            ));

            netExam.addQuestion(new Question(
                    "What is the standard well-known default port number used by unencrypted HTTP web communication?",
                    "21",
                    "25",
                    "80",
                    "443",
                    "C",
                    1
            ));

            netExam.addQuestion(new Question(
                    "Which protocol is used to resolve a known logical IP address to a physical MAC hardware address?",
                    "ARP",
                    "DHCP",
                    "DNS",
                    "NAT",
                    "A",
                    1
            ));

            netExam.addQuestion(new Question(
                    "What is the address length in bits of an Internet Protocol Version 4 (IPv4) address?",
                    "32 bits",
                    "64 bits",
                    "128 bits",
                    "256 bits",
                    "A",
                    1
            ));

            netExam.recalculateTotalMarks();
            examRepository.save(netExam);

            logger.info("Sample examinations initialized successfully.");
        }
    }
}
