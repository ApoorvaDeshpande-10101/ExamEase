package com.examease.repository;

import com.examease.exception.DataStorageException;
import com.examease.model.User;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private static final String FILE_PATH = "data/users.dat";
    private final Object lock = new Object();

    @SuppressWarnings("unchecked")
    public List<User> findAll() {
        synchronized (lock) {
            File file = new File(FILE_PATH);

            if (!file.exists() || file.length() == 0) {
                return new ArrayList<>();
            }

            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
                return (List<User>) in.readObject();
            } catch (FileNotFoundException e) {
                return new ArrayList<>();
            } catch (IOException | ClassNotFoundException e) {
                throw new DataStorageException("Unable to read user data from file: " + FILE_PATH, e);
            }
        }
    }

    public Optional<User> findById(String id) {
        if (id == null) return Optional.empty();
        return findAll().stream()
                .filter(u -> id.equals(u.getId()))
                .findFirst();
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return findAll().stream()
                .filter(user -> user.getEmail().equalsIgnoreCase(email.trim()))
                .findFirst();
    }

    public void save(User user) {
        synchronized (lock) {
            List<User> users = findAll();
            // Update if already exists, else append
            boolean updated = false;
            for (int i = 0; i < users.size(); i++) {
                if (users.get(i).getId().equals(user.getId()) ||
                    users.get(i).getEmail().equalsIgnoreCase(user.getEmail())) {
                    users.set(i, user);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                users.add(user);
            }
            saveAll(users);
        }
    }

    public void deleteById(String id) {
        synchronized (lock) {
            List<User> users = findAll();
            users.removeIf(u -> u.getId().equals(id));
            saveAll(users);
        }
    }

    public void saveAll(List<User> users) {
        synchronized (lock) {
            File file = new File(FILE_PATH);

            try {
                if (file.getParentFile() != null && !file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }

                try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
                    out.writeObject(new ArrayList<>(users));
                }
            } catch (IOException e) {
                throw new DataStorageException("Unable to write user data to file: " + FILE_PATH, e);
            }
        }
    }
}
