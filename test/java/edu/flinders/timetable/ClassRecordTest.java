package edu.flinders.timetable;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

import java.beans.Transient;

class ClassRecordTest {
    private ClassRecord createRecord() {
        ClassRecord r = new ClassRecord();
        r.topicCode = "COMP1701";
        r.topicName = "Game Design";
        r.attendanceMode = "In person";
        r.campus = "Tonsley";
        r.semester = "2";
        r.availabilityNumber = "1";
        r.className = "Workshop";
        r.classInstance = "1";
        r.firstClassDate = "29 Jul";
        r.lastClassDate = "16 Sep";
        r.day = "Wednesday";
        r.startTime = "10:00";
        r.endTime = "11:00";
        r.building = "Tonsley T1";
        r.room = "1.08 Lecture Room";
        return r;
    }

    @Test
    @DisplayName("Test if fields are correctly assigned")
    void TestFieldCorrect() {
        ClassRecord r = createRecord();
        assertEquals("COMP1701", r.topicCode);
        assertEquals("Game Design", r.topicName);
        assertEquals("Tonsley", r.campus);
        assertEquals("2", r.semester);
        assertEquals("Wednesday", r.day);
    }

    @Test
    @DisplayName("Test import key generation")
    void TestImportKey() {
        ClassRecord r = createRecord();
        String key = r.importKey();
        assertTrue(key.contains("comp1701"));
        assertTrue(key.contains("Wednesday"));
    }

    @Test
    @DisplayName("Returns False for non lectures")
    void TestLectureFalse() {
        ClassRecord r = createRecord();
        assertFalse(r.isLecture());
    }

    @Test
    @DisplayName("Group Key excludes date and class type")
    void TestDateExclude() {
        ClassRecord r = createRecord();
        String key = r.groupKey();
        assertFalse(key.contains("wednesday"));
        assertFalse(key.contains("workshop"));
    }

    @Test
    @DisplayName("Group Key includes topic code and campus")
    void TestGroupKey() {
        ClassRecord r = createRecord();
        String key = r.groupKey();
        assertTrue(key.contains("comp1701"));
        assertTrue(key.contains("tonsley"));
    }

    @Test
    @DisplayName("returns topic code and name combined)")
    void testFullTopic() {
        ClassRecord r = createRecord();
        String result = r.topicFull();
        assertTrue(result.contains("COMP1701"));
        assertTrue(result.contains("Game Design"));
    }

    @Test
    @DisplayName("location combines building and room")
    void testLocationCapacity() {
        ClassRecord r = createRecord();
        assertEquals("Tonsley T1 1.008 Lecture Room", r.locationFull());
    }

    @Test
    @DisplayName("returns true for Lecture")
    void testIsLecture() {
        ClassRecord r = createRecord();
        r.className = "Lecture";
        assertTrue(r.isLecture());
    }

    @Test
    @DisplayName



