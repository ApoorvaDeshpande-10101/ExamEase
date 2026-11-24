# ExamEase – Online Examination System

**ExamEase** is a modern, web-based Online Examination System developed with Java and Spring Boot. It provides an intuitive, distraction-free examination environment for students and a comprehensive administrative suite for educators and academic administrators.

The project demonstrates core Java engineering concepts without relying on external relational or NoSQL database servers, utilizing native **Java Object Serialization** for persistent storage.

---

## 🚀 Key Features

### For Students
- **Account Registration & Authentication:** Secure student registration with BCrypt password hashing.
- **Student Dashboard:** Personal statistics (exams attempted, average score, top score), available exams, and recent attempt history.
- **Examination Instructions:** Detailed pre-exam briefs displaying question count, duration, passing criteria, and rules.
- **Distraction-Free Examination Interface:**
  - Real-time countdown timer synchronized with backend server deadlines.
  - Interactive Question Navigation Palette with live color coding:
    - 🟢 **Answered**
    - 🟡 **Marked for Review**
    - 🟣 **Answered & Marked for Review**
    - ⚪ **Unanswered**
  - Asynchronous background auto-saving of selected answers.
  - Automatic submission when time expires.
  - Manual submission confirmation modal with detailed answer summaries.
- **Instant Result Evaluation & Academic Breakdown:**
  - Server-side calculation of marks, percentage, and PASS/FAIL status.
  - Question-by-question review showing student's choice, correct answer, and marks awarded.
- **Result History:** Complete record of all past attempts and performance metrics.

### For Administrators
- **Default Administrator Account:**
  - **Email:** `admin@examease.com`
  - **Password:** `Admin@123`
- **Administrator Dashboard:** High-level platform statistics, total registered students, total exams, total attempts, platform average score, pass rate, and real-time multithreaded event notifications.
- **Examination Management:**
  - Create, edit, and delete examinations.
  - Add, edit, and delete multiple-choice questions (4 options, correct answer, marks).
  - One-click Publish / Unpublish toggling (validates question count before publishing).
  - Automatic recalculation of total marks when questions are modified.
- **Student Management:**
  - Searchable student directory (filtered via Java Streams API).
  - Inspect individual student academic history and examination breakdowns.
- **Academic Analytics & Reports:**
  - Overall performance metrics.
  - Examination-wise performance breakdown (attempts, passed, failed, pass rate, average score, top score).
  - Top-scoring submissions ranking.

---

## 🛠️ Technology Stack

- **Backend:** Java 25 / 26, Spring Boot 4.2.0, Spring MVC, Spring Security Crypto (BCrypt)
- **Persistence:** Java Object Serialization (`ObjectOutputStream`, `ObjectInputStream`, `.dat` files)
- **Concurrency:** Java `ScheduledExecutorService`, `CompletableFuture`, `ExecutorService`
- **Analytics:** Java Streams API (`filter`, `map`, `sorted`, `collect`, `count`, `average`, `max`, `groupingBy`)
- **Frontend:** HTML5, CSS3, JavaScript (Vanilla ES6), Thymeleaf Template Engine
- **Build Tool:** Apache Maven 3.10.1 (Maven Wrapper included)

---

## 📚 Mandatory Java Academic Requirements

### 1. Packages and Modular Architecture
Organized strictly into clean, dedicated packages:
```
com.examease
├── config          # Security interceptor, resource handlers, admin initializer
├── controller      # MVC controllers (Home, Auth, Student, Admin, Exam, Result)
├── exception       # Custom domain exceptions & GlobalExceptionHandler (@ControllerAdvice)
├── model           # Serializable entities (User, Exam, Question, ExamAttempt, Result, Notification)
├── repository      # Synchronized file serialization repositories
├── service         # Core business logic, analytics, and concurrency services
└── ExameaseApplication.java
```

### 2. File Handling and Java Object Serialization
- **Zero External Databases:** All records persist in binary `.dat` files within the `data/` folder:
  - `data/users.dat`
  - `data/exams.dat`
  - `data/attempts.dat`
  - `data/results.dat`
  - `data/notifications.dat`
- **Implementation Highlights:**
  - Model classes implement `java.io.Serializable` with `serialVersionUID`.
  - Repositories utilize `ObjectOutputStream` and `ObjectInputStream` wrapped in try-with-resources.
  - Thread-safe repository operations using internal object locks (`synchronized`) to prevent concurrent file corruption.
  - Automatic directory and file recovery mechanisms throwing custom `DataStorageException`.
  - Passwords are encrypted with BCrypt prior to serialization.

### 3. Java Streams API
Implemented extensively in `AnalyticsService` and controllers:
- `filter()`: Filtering published vs draft exams; searching students by name/email keyword.
- `map()` / `mapToDouble()`: Extracting percentages, scores, and exam identifiers.
- `sorted()`: Dynamic sorting of examination results descending by score or date.
- `collect()` / `collect(groupingBy())`: Grouping attempts and results per examination.
- `count()`: Calculating passed, failed, and attempted counts.
- `average()`: Computing platform-wide and exam-specific score averages.
- `max()`: Determining the top student score and the most attempted examination.

### 4. Multithreading and Concurrency
Implemented in `BackgroundTaskService`:
- **Deadline Monitoring (`ScheduledExecutorService`):**
  - Background daemon thread runs periodically (every 10 seconds).
  - Checks for active exam attempts where `LocalDateTime.now().isAfter(attempt.getDeadline())`.
  - Automatically evaluates marks, saves results, and notifies administrators if a student closes their browser.
- **Asynchronous Auto-Saving (`CompletableFuture` & `ExecutorService`):**
  - Student answer updates are offloaded from HTTP request threads to a background thread pool, guaranteeing instantaneous response times while ensuring disk persistence.
- **Background Event Notification Pipeline:**
  - Asynchronously logs system events (`STUDENT_REGISTERED`, `EXAM_STARTED`, `EXAM_SUBMITTED`, `TIMER_EXPIRED`) to `notifications.dat`.
- **Graceful Lifecycle Management:**
  - `@PreDestroy` method ensures executors await termination and shut down cleanly when the application stops.

### 5. Exception Handling
- **Custom Exception Classes:**
  - `UserAlreadyExistsException`
  - `InvalidCredentialsException`
  - `ExamNotFoundException`
  - `ExamAlreadySubmittedException`
  - `InvalidExamDataException`
  - `UnauthorizedException`
  - `DataStorageException`
- **Global Exception Handler:**
  - Annotated with `@ControllerAdvice` (`GlobalExceptionHandler.java`).
  - Intercepts business exceptions and maps them to user-friendly UI views and redirect flash attributes rather than exposing internal stack traces.

---

## 🏃 Getting Started

### Prerequisites
- JDK 25 or compatible (JDK 26 tested)
- Maven Wrapper (`mvnw.cmd`) is included in the project directory.

### Running the Application

1. Open a terminal in the project directory:
   ```powershell
   cd D:\ExamEase2.0
   ```

2. Run the application using the Maven Wrapper:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

3. Access the application in your browser:
   ```
   http://localhost:8080/
   ```

### Running Tests
Execute the unit and integration test suite:
```powershell
.\mvnw.cmd test
```

---

## 🧪 Testing Credentials

| Role | Email | Password |
|---|---|---|
| **Administrator** | `admin@examease.com` | `Admin@123` |
| **Sample Student** | Register via `/register` | Minimum 6 characters |

---

## 📄 License
Academic Mini Project for Java & Spring Boot Full-Stack Demonstration.
© 2026 ExamEase.
