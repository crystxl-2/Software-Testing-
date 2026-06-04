package edu.flinders.timetable;

import java.io.Serializable;
import java.util.*;

public class GenerationSettings implements Serializable {
    private static final long serialVersionUID = 1L;
    public String timetableName = "";
    public Set<String> semesters = new LinkedHashSet<>();
    public Set<String> topics = new LinkedHashSet<>();
    public Set<String> campuses = new LinkedHashSet<>();
    public boolean allowLectureOverlap = false;
    public List<String> preferences = new ArrayList<>();
}
