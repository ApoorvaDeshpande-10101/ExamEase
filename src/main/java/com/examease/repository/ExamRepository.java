package com.examease.repository;

import com.examease.exception.DataStorageException;
import com.examease.model.Exam;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ExamRepository {

    private static final String FILE_PATH = "data/exams.dat";
    private final Object lock = new Object();

    @SuppressWarnings("unchecked")
    public List<Exam> findAll() {
        synchronized (lock) {
            File file = new File(FILE_PATH);

            if (!file.exists() || file.length() == 0) {
                return new ArrayList<>();
            }

            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
                return (List<Exam>) in.readObject();
            } catch (FileNotFoundException e) {
                return new ArrayList<>();
            } catch (IOException | ClassNotFoundException e) {
                throw new DataStorageException("Unable to read exams data from file: " + FILE_PATH, e);
            }
        }
    }

    public Optional<Exam> findById(String id) {
        if (id == null) return Optional.empty();
        return findAll().stream()
                .filter(exam -> exam.getId().equals(id))
                .findFirst();
    }

    public List<Exam> findByPublished(boolean published) {
        return findAll().stream()
                .filter(exam -> exam.isPublished() == published)
                .toList();
    }

    public void save(Exam exam) {
        synchronized (lock) {
            List<Exam> exams = findAll();
            boolean updated = false;
            for (int i = 0; i < exams.size(); i++) {
                if (exams.get(i).getId().equals(exam.getId())) {
                    exams.set(i, exam);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                exams.add(exam);
            }
            saveAll(exams);
        }
    }

    public void deleteById(String id) {
        synchronized (lock) {
            List<Exam> exams = findAll();
            exams.removeIf(e -> e.getId().equals(id));
            saveAll(exams);
        }
    }

    public void saveAll(List<Exam> exams) {
        synchronized (lock) {
            File file = new File(FILE_PATH);

            try {
                if (file.getParentFile() != null && !file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }

                try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
                    out.writeObject(new ArrayList<>(exams));
                }
            } catch (IOException e) {
                throw new DataStorageException("Unable to write exams data to file: " + FILE_PATH, e);
            }
        }
    }
}
