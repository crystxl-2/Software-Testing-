package edu.flinders.timetable;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

public class Timetable implements Serializable {
    private static final long serialVersionUID = 1L;
    public String id = UUID.randomUUID().toString();
    public String name;
    public LocalDateTime createdAt = LocalDateTime.now();
    public List<ClassRecord> classes = new ArrayList<>();

    public Timetable(String name, List<ClassRecord> classes) {
        this.name = name;
        this.classes.addAll(classes);
    }
}
