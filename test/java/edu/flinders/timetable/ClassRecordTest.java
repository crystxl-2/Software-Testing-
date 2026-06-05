package edu.flinders.timetable;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.beans.Transient;

import org.junit.jupiter.api.DisplayName;

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
    @Order(1)
    @Name("Carl De Guzman")
    @Tag("Critical")
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
    @Order(2)
    @Name("Carl De Guzman")
    @Tag("Core")
    @DisplayName("Returns availability in correct format")
    void TestAvailabilityFull() {
        ClassRecord r = createRecord();
        String result = r.availabilityFull();
        assertEquals("In person - Tonsley - S2 - 1", result);
    }

    @Test
    @Order(3)
    @Name("Carl De Guzman")
    @Tag("Core")
    @DisplayName("Test import key generation")
    void TestImportKey() {
        ClassRecord r = createRecord();
        String key = r.importKey();
        assertTrue(key.contains("comp1701"));
        assertTrue(key.contains("wednesday"));
    }

    @Test
    @Order(4)
    @Name("Carl De Guzman")
    @Tag("Core")
    @DisplayName("Returns False for non lectures")
    void TestLectureFalse() {
        ClassRecord r = createRecord();
        assertFalse(r.isLecture());
    }

    @Test
    @Order(5)
    @Name("Carl De Guzman")
    @Tag("Core")
    @DisplayName("Group Key excludes date and class type")
    void TestDateExclude() {
        ClassRecord r = createRecord();
        String key = r.groupKey();
        assertFalse(key.contains("wednesday"));
        assertFalse(key.contains("workshop"));
    }

    @Test
    @Order(6)
    @Name("Carl De Guzman")
    @Tag("Additional")
    @DisplayName("Group Key includes topic code and campus")
    void TestGroupKey() {
        ClassRecord r = createRecord();
        String key = r.groupKey();
        assertTrue(key.contains("comp1701"));
        assertTrue(key.contains("tonsley"));
    }

    @Test
    @Order(7)
    @Name("Carl De Guzman")
    @Tag("Core")
    @DisplayName("returns topic code and name combined)")
    void testFullTopic() {
        ClassRecord r = createRecord();
        String result = r.topicFull();
        assertTrue(result.contains("COMP1701"));
        assertTrue(result.contains("Game Design"));
    }

    @Test
    @Order(8)
    @Name("Carl De Guzman")
    @Tag("Core")
    @DisplayName("location combines building and room")
    void testLocationCapacity() {
        ClassRecord r = createRecord();
        assertEquals("Tonsley T1, 1.08 Lecture Room", r.locationFull());
    }

    @Test
    @Order(9)
    @Name("Carl De Guzman")
    @Tag("Additional")
    @DisplayName("returns true for Lecture")
    void testIsLecture() {
        ClassRecord r = createRecord();
        r.className = "Lecture";
        assertTrue(r.isLecture());
    }

    @Test
    @Order(10)
    @Name("Carl De Guzman")
    @Tag("Additional")
    @DisplayName("Check Class time is correctly formatted")
    void testClassTime() {
        ClassRecord r = createRecord();
        String time = r.startTime + " - " + r.endTime;
        assertEquals("10:00 - 11:00", time);
    }

    @Test
    @Order(11)
    @Name("Carl De Guzman")
    @Tag("Core")
    @DisplayName("Throw an error code if topic code is null")
    void testNullTopicCode() {
        ClassRecord r = new ClassRecord();
        r.topicCode = null;
        assertThrows(IllegalArgumentException.class, r::validate);
    }


    @Test
    @Order(12)
    @Name("Carl De Guzman")
    @Tag("Critical")
    @DisplayName("Throw an error for any random/invalid fields given")
    void testRandomFields() {
        ClassRecord r = createRecord();
        assertThrows(IllegalArgumentException.class, () -> set.Field("RandomField", "value"));
    }
}