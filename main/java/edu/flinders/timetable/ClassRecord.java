package edu.flinders.timetable;

import java.io.Serializable;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ClassRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    public String id;
    public String topicCode;
    public String topicName;
    public String attendanceMode;
    public String campus;
    public String semester;
    public String availabilityNumber;
    public String className;
    public String classInstance;
    public String firstClassDate;
    public String lastClassDate;
    public String day;
    public String startTime;
    public String endTime;
    public String building;
    public String room;

    public ClassRecord() { this.id = UUID.randomUUID().toString(); }

    public static ClassRecord fromCsvRow(Map<String, String> row) {
        String[] required = {"Topic", "Availability", "Class", "Class instance", "Date", "Day", "Time"};
        for (String h : required) requireValue(row, h);
        String location = getValue(row, "Location", "Room");
        if (location.isBlank()) throw new IllegalArgumentException("Missing required value: Location/Room");

        ClassRecord r = new ClassRecord();
        parseTopic(getValue(row, "Topic"), r);
        parseAvailability(getValue(row, "Availability"), r);
        r.className = clean(getValue(row, "Class"));
        r.classInstance = clean(getValue(row, "Class instance"));
        parseDateRange(getValue(row, "Date"), r);
        r.day = clean(getValue(row, "Day"));
        parseTimeRange(getValue(row, "Time"), r);
        parseLocation(location, r);
        r.validate();
        return r;
    }

    private static void parseTopic(String topic, ClassRecord r) {
        topic = clean(topic);
        String[] parts = topic.split("\\s+", 2);
        if (parts.length < 2) throw new IllegalArgumentException("Topic must contain code and name: " + topic);
        r.topicCode = parts[0].trim();
        r.topicName = parts[1].trim();
    }

    private static void parseAvailability(String availability, ClassRecord r) {
        String[] parts = clean(availability).split("\\s+-\\s+");
        if (parts.length < 4) throw new IllegalArgumentException("Availability must contain mode, campus, semester, number: " + availability);
        r.attendanceMode = parts[0].trim();
        r.campus = parts[1].trim();
        r.semester = parts[2].trim().replace("S", "");
        r.availabilityNumber = parts[3].trim();
    }

    private static void parseDateRange(String date, ClassRecord r) {
        String[] parts = clean(date).split("\\s+-\\s+", 2);
        if (parts.length < 2) throw new IllegalArgumentException("Date must be a range: " + date);
        r.firstClassDate = parts[0].trim();
        r.lastClassDate = parts[1].trim();
    }

    private static void parseTimeRange(String time, ClassRecord r) {
        String[] parts = clean(time).split("\\s+-\\s+", 2);
        if (parts.length < 2) throw new IllegalArgumentException("Time must be a range: " + time);
        r.startTime = parts[0].trim();
        r.endTime = parts[1].trim();
        r.startAsTime();
        r.endAsTime();
    }

    private static void parseLocation(String location, ClassRecord r) {
        String[] parts = clean(location).split(",", 2);
        r.building = parts[0].trim();
        r.room = parts.length > 1 ? parts[1].trim() : "";
    }

    public void validate() {
        List<String> values = Arrays.asList(topicCode, topicName, attendanceMode, campus, semester, availabilityNumber, className, classInstance, firstClassDate, lastClassDate, day, startTime, endTime, building);
        if (values.stream().anyMatch(v -> v == null || v.trim().isEmpty())) throw new IllegalArgumentException("A required class field is blank.");
    }

    public String importKey() {
        return key(topicCode, topicName, attendanceMode, campus, semester, availabilityNumber, className, classInstance, firstClassDate, lastClassDate, day);
    }

    public String groupKey() {
        return key(topicCode, topicName, attendanceMode, campus, semester, availabilityNumber, className, classInstance);
    }

    public String swapKey() { return key(topicCode, className); }

    public boolean isLecture() { return className != null && className.toLowerCase().contains("lecture"); }

    public LocalTime startAsTime() { return LocalTime.parse(startTime, DateTimeFormatter.ofPattern("H:mm")); }
    public LocalTime endAsTime() { return LocalTime.parse(endTime, DateTimeFormatter.ofPattern("H:mm")); }

    public String topicFull() { return topicCode + " " + topicName; }
    public String locationFull() { return building + (room.isBlank() ? "" : ", " + room); }
    public String availabilityFull() { return attendanceMode + " - " + campus + " - S" + semester + " - " + availabilityNumber; }

    public String shortLine() {
        return String.format("%s | %s | %s #%s | %s | S%s | %s", topicFull(), campus, className, classInstance, day, semester, locationFull());
    }

    public String detailedLine() {
        return String.format("%s | %s | %s #%s | %s to %s | %s | %s-%s | %s", topicFull(), availabilityFull(), className, classInstance, firstClassDate, lastClassDate, day, startTime, endTime, locationFull());
    }

    public String toCsvRow() {
        return CsvUtil.escape(topicCode + " " + topicName) + "," + CsvUtil.escape(availabilityFull()) + "," + CsvUtil.escape(className) + "," + CsvUtil.escape(classInstance) + "," + CsvUtil.escape(firstClassDate + " - " + lastClassDate) + "," + CsvUtil.escape(day) + "," + CsvUtil.escape(startTime + " - " + endTime) + "," + CsvUtil.escape(locationFull());
    }

    public boolean matches(Map<String, String> criteria) {
        for (Map.Entry<String, String> e : criteria.entrySet()) {
            String expected = e.getValue().toLowerCase(Locale.ROOT).trim();
            if (expected.isEmpty()) continue;
            String actual = getField(e.getKey()).toLowerCase(Locale.ROOT);
            if (!actual.contains(expected)) return false;
        }
        return true;
    }

    public String getField(String field) {
        return switch (field) {
            case "topicCode" -> topicCode;
            case "topicName" -> topicName;
            case "attendanceMode" -> attendanceMode;
            case "campus" -> campus;
            case "semester" -> semester;
            case "availabilityNumber" -> availabilityNumber;
            case "className" -> className;
            case "classInstance" -> classInstance;
            case "firstClassDate" -> firstClassDate;
            case "lastClassDate" -> lastClassDate;
            case "day" -> day;
            case "startTime" -> startTime;
            case "endTime" -> endTime;
            case "building" -> building;
            case "room" -> room;
            default -> "";
        };
    }

    public void setField(String field, String value) {
        value = clean(value);
        switch (field) {
            case "topicCode" -> topicCode = value;
            case "topicName" -> topicName = value;
            case "attendanceMode" -> attendanceMode = value;
            case "campus" -> campus = value;
            case "semester" -> semester = value.replace("S", "");
            case "availabilityNumber" -> availabilityNumber = value;
            case "className" -> className = value;
            case "classInstance" -> classInstance = value;
            case "firstClassDate" -> firstClassDate = value;
            case "lastClassDate" -> lastClassDate = value;
            case "day" -> day = value;
            case "startTime" -> { startTime = value; startAsTime(); }
            case "endTime" -> { endTime = value; endAsTime(); }
            case "building" -> building = value;
            case "room" -> room = value;
            default -> throw new IllegalArgumentException("Unknown field: " + field);
        }
        validate();
    }

    private static void requireValue(Map<String, String> row, String header) {
        if (getValue(row, header).isBlank()) throw new IllegalArgumentException("Missing required value: " + header);
    }

    private static String getValue(Map<String, String> row, String... headers) {
        for (String header : headers) {
            for (Map.Entry<String, String> entry : row.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(header)) return clean(entry.getValue());
            }
        }
        return "";
    }

    private static String clean(String s) { return s == null ? "" : s.trim(); }
    private static String key(String... parts) { return String.join("|", Arrays.stream(parts).map(p -> p == null ? "" : p.trim().toLowerCase(Locale.ROOT)).toList()); }
}
