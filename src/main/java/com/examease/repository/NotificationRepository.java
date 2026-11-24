package com.examease.repository;

import com.examease.exception.DataStorageException;
import com.examease.model.Notification;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class NotificationRepository {

    private static final String FILE_PATH = "data/notifications.dat";
    private final Object lock = new Object();

    @SuppressWarnings("unchecked")
    public List<Notification> findAll() {
        synchronized (lock) {
            File file = new File(FILE_PATH);

            if (!file.exists() || file.length() == 0) {
                return new ArrayList<>();
            }

            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
                return (List<Notification>) in.readObject();
            } catch (FileNotFoundException e) {
                return new ArrayList<>();
            } catch (IOException | ClassNotFoundException e) {
                throw new DataStorageException("Unable to read notification data from file: " + FILE_PATH, e);
            }
        }
    }

    public void save(Notification notification) {
        synchronized (lock) {
            List<Notification> notifications = findAll();
            notifications.add(0, notification); // Latest first
            if (notifications.size() > 50) { // Keep last 50
                notifications = new ArrayList<>(notifications.subList(0, 50));
            }
            saveAll(notifications);
        }
    }

    public void saveAll(List<Notification> notifications) {
        synchronized (lock) {
            File file = new File(FILE_PATH);

            try {
                if (file.getParentFile() != null && !file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }

                try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
                    out.writeObject(new ArrayList<>(notifications));
                }
            } catch (IOException e) {
                throw new DataStorageException("Unable to write notification data to file: " + FILE_PATH, e);
            }
        }
    }
}
