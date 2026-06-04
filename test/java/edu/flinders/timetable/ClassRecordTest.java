package edu.flinders.timetable;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClassRecordTest {
    private ClassRecord CreateRecord() {
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
    void TestFieldCorrect() {
        ClassRecordTest r = CreateRecord();
        assertEquals("COMP1701", r.topicCode);
        assertEquals("Game Design", r.topicName);
        assertEquals("Tonsley", r.campus);
        assertEquals("2", r.semester);
        assertEquals("Wednesday", r.day);
    }
}
    @Test
    void TestImportKey() {
        ClassRecord r = CreateRecord();
        String key = r.importKey();
        assertTrue(key.contains("comp1701"));
        assertTrue(key.contains("Wednesday"));
    }

    @Test
    void TestLectureFalse() {
        ClassRecord r = CreateRecord();
        assertFalse(r.isLecture));
    }

    @Test
    void TestDateExclude() {
        ClassRecord r = ClassRecord();
        String key = r.groupKey();
        assertFalse(key.contains("wednesday"));
        assertFalse(key.contains("workshop"));
    }
}
