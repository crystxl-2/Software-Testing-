package edu.flinders.timetable;

import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class ClassService {
    private final DataStore store;
    public ClassService(DataStore store) { this.store = store; }

    public ImportResult importCsv(String filePath) throws Exception {
        List<Map<String, String>> rows = CsvUtil.readCsv(Paths.get(filePath));
        ImportResult result = new ImportResult();
        for (Map<String, String> row : rows) {
            ClassRecord incoming = ClassRecord.fromCsvRow(row);
            Optional<ClassRecord> existing = store.records.stream().filter(r -> r.importKey().equals(incoming.importKey())).findFirst();
            if (existing.isPresent()) {
                ClassRecord r = existing.get();
                r.startTime = incoming.startTime;
                r.endTime = incoming.endTime;
                r.building = incoming.building;
                r.room = incoming.room;
                result.updated++;
            } else {
                store.records.add(incoming);
                result.created++;
            }
        }
        store.save();
        return result;
    }

    public List<ClassRecord> search(Map<String, String> criteria) {
        return store.records.stream().filter(r -> r.matches(criteria)).sorted(classComparator()).toList();
    }

    public Map<String, List<ClassRecord>> grouped(List<ClassRecord> records) {
        return records.stream().collect(Collectors.groupingBy(ClassRecord::groupKey, LinkedHashMap::new, Collectors.toList()));
    }

    public Optional<ClassRecord> findById(String id) { return store.records.stream().filter(r -> r.id.equals(id)).findFirst(); }

    public boolean deleteById(String id) {
        boolean removed = store.records.removeIf(r -> r.id.equals(id));
        if (removed) {
            for (Timetable t : store.timetables) t.classes.removeIf(r -> r.id.equals(id));
            store.save();
        }
        return removed;
    }

    public void save() { store.save(); }

    public static Comparator<ClassRecord> classComparator() {
        List<String> days = List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
        return Comparator.comparing((ClassRecord r) -> r.topicCode)
                .thenComparing(r -> r.className)
                .thenComparing(r -> r.classInstance)
                .thenComparingInt(r -> days.indexOf(r.day) < 0 ? 99 : days.indexOf(r.day))
                .thenComparing(ClassRecord::startAsTime);
    }
}
