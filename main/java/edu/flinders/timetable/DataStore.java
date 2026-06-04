package edu.flinders.timetable;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class DataStore implements Serializable {
    private static final long serialVersionUID = 1L;
    public List<ClassRecord> records = new ArrayList<>();
    public List<Timetable> timetables = new ArrayList<>();
    public GenerationSettings lastSettings = new GenerationSettings();

    private static final Path STORE_FILE = Paths.get("timetable-data.ser");

    public static DataStore load() {
        if (!Files.exists(STORE_FILE)) return new DataStore();
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(STORE_FILE))) {
            return (DataStore) in.readObject();
        } catch (Exception e) {
            System.out.println("Could not load saved data. Starting with empty storage. " + e.getMessage());
            return new DataStore();
        }
    }

    public void save() {
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(STORE_FILE))) {
            out.writeObject(this);
        } catch (IOException e) {
            System.out.println("Warning: could not save data: " + e.getMessage());
        }
    }
}
