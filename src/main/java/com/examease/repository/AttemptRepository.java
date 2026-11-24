package com.examease.repository;

import com.examease.exception.DataStorageException;
import com.examease.model.ExamAttempt;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class AttemptRepository {

    private static final String FILE_PATH = "data/attempts.dat";
    private final Object lock = new Object();

    @SuppressWarnings("unchecked")
    public List<ExamAttempt> findAll() {
        synchronized (lock) {
            File file = new File(FILE_PATH);

            if (!file.exists() || file.length() == 0) {
                return new ArrayList<>();
            }

            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
                return (List<ExamAttempt>) in.readObject();
            } catch (FileNotFoundException e) {
                return new ArrayList<>();
            } catch (IOException | ClassNotFoundException e) {
                throw new DataStorageException("Unable to read attempts data from file: " + FILE_PATH, e);
            }
        }
    }

    public Optional<ExamAttempt> findById(String id) {
        if (id == null) return Optional.empty();
        return findAll().stream()
                .filter(a -> a.getId().equals(id))
                .findFirst();
    }

    public List<ExamAttempt> findByStudentId(String studentId) {
        if (studentId == null) return new ArrayList<>();
        return findAll().stream()
                .filter(a -> studentId.equals(a.getStudentId()))
                .toList();
    }

    public List<ExamAttempt> findByExamId(String examId) {
        if (examId == null) return new ArrayList<>();
        return findAll().stream()
                .filter(a -> examId.equals(a.getExamId()))
                .toList();
    }

    public Optional<ExamAttempt> findActiveAttempt(String studentId, String examId) {
        if (studentId == null || examId == null) return Optional.empty();
        return findAll().stream()
                .filter(a -> studentId.equals(a.getStudentId()) && examId.equals(a.getExamId()) && !a.isSubmitted())
                .findFirst();
    }

    public void save(ExamAttempt attempt) {
        synchronized (lock) {
            List<ExamAttempt> attempts = findAll();
            boolean updated = false;
            for (int i = 0; i < attempts.size(); i++) {
                if (attempts.get(i).getId().equals(attempt.getId())) {
                    attempts.set(i, attempt);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                attempts.add(attempt);
            }
            saveAll(attempts);
        }
    }

    public void saveAll(List<ExamAttempt> attempts) {
        synchronized (lock) {
            File file = new File(FILE_PATH);

            try {
                if (file.getParentFile() != null && !file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }

                try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
                    out.writeObject(new ArrayList<>(attempts));
                }
            } catch (IOException e) {
                throw new DataStorageException("Unable to write attempts data to file: " + FILE_PATH, e);
            }
        }
    }
}
