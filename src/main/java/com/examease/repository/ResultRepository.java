package com.examease.repository;

import com.examease.exception.DataStorageException;
import com.examease.model.Result;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ResultRepository {

    private static final String FILE_PATH = "data/results.dat";
    private final Object lock = new Object();

    @SuppressWarnings("unchecked")
    public List<Result> findAll() {
        synchronized (lock) {
            File file = new File(FILE_PATH);

            if (!file.exists() || file.length() == 0) {
                return new ArrayList<>();
            }

            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
                return (List<Result>) in.readObject();
            } catch (FileNotFoundException e) {
                return new ArrayList<>();
            } catch (IOException | ClassNotFoundException e) {
                throw new DataStorageException("Unable to read results data from file: " + FILE_PATH, e);
            }
        }
    }

    public Optional<Result> findById(String id) {
        if (id == null) return Optional.empty();
        return findAll().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst();
    }

    public List<Result> findByStudentId(String studentId) {
        if (studentId == null) return new ArrayList<>();
        return findAll().stream()
                .filter(r -> studentId.equals(r.getStudentId()))
                .toList();
    }

    public List<Result> findByExamId(String examId) {
        if (examId == null) return new ArrayList<>();
        return findAll().stream()
                .filter(r -> examId.equals(r.getExamId()))
                .toList();
    }

    public Optional<Result> findLatestByStudentIdAndExamId(String studentId, String examId) {
        if (studentId == null || examId == null) return Optional.empty();
        return findAll().stream()
                .filter(r -> studentId.equals(r.getStudentId()) && examId.equals(r.getExamId()))
                .sorted((a, b) -> b.getSubmissionDate().compareTo(a.getSubmissionDate()))
                .findFirst();
    }

    public void save(Result result) {
        synchronized (lock) {
            List<Result> results = findAll();
            boolean updated = false;
            for (int i = 0; i < results.size(); i++) {
                if (results.get(i).getId().equals(result.getId())) {
                    results.set(i, result);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                results.add(result);
            }
            saveAll(results);
        }
    }

    public void saveAll(List<Result> results) {
        synchronized (lock) {
            File file = new File(FILE_PATH);

            try {
                if (file.getParentFile() != null && !file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }

                try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
                    out.writeObject(new ArrayList<>(results));
                }
            } catch (IOException e) {
                throw new DataStorageException("Unable to write results data to file: " + FILE_PATH, e);
            }
        }
    }
}
