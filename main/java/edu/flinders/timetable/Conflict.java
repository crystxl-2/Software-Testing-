package edu.flinders.timetable;

public class Conflict {
    public final ClassRecord first;
    public final ClassRecord second;
    public final String message;
    public Conflict(ClassRecord first, ClassRecord second, String message) {
        this.first = first; this.second = second; this.message = message;
    }
}
