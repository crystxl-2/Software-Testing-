package edu.flinders.timetable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class TimetableService {
    private final DataStore store;
    public TimetableService(DataStore store) { this.store = store; }

    public Timetable generate(GenerationSettings settings) {
        if (settings.topics.isEmpty()) throw new IllegalArgumentException("At least one topic must be selected.");
        if (settings.semesters.isEmpty()) settings.semesters.addAll(List.of("1", "2"));
        if (settings.campuses.isEmpty()) settings.campuses.addAll(List.of("Bedford Park", "Tonsley", "Flinders City Campus"));

        Map<String, List<ClassRecord>> byTopic = store.records.stream()
                .filter(r -> settings.topics.contains(r.topicFull()))
                .filter(r -> settings.semesters.contains(r.semester))
                .filter(r -> settings.campuses.contains(r.campus))
                .collect(Collectors.groupingBy(ClassRecord::topicFull, LinkedHashMap::new, Collectors.toList()));

        List<List<List<ClassRecord>>> topicOptions = new ArrayList<>();
        for (String topic : settings.topics) {
            List<ClassRecord> topicRecords = byTopic.getOrDefault(topic, List.of());
            if (topicRecords.isEmpty()) throw new IllegalArgumentException("No class records found for selected topic and filters: " + topic);
            List<List<ClassRecord>> options = buildTopicOptions(topicRecords, settings);
            if (options.isEmpty()) throw new IllegalArgumentException("No valid class combinations found for: " + topic);
            topicOptions.add(options);
        }

        Candidate best = new Candidate();
        backtrack(topicOptions, 0, new ArrayList<>(), settings, best);
        if (best.records.isEmpty()) throw new IllegalArgumentException("No timetable could be generated from the current selections.");

        String name = uniqueName(settings.timetableName == null || settings.timetableName.isBlank() ? "Timetable" : settings.timetableName.trim());
        Timetable t = new Timetable(name, best.records.stream().sorted(ClassService.classComparator()).toList());
        store.timetables.add(t);
        store.lastSettings = settings;
        store.save();
        return t;
    }

    private List<List<ClassRecord>> buildTopicOptions(List<ClassRecord> records, GenerationSettings settings) {
        List<List<ClassRecord>> combinations = new ArrayList<>();

        List<ClassRecord> cityOnly = records.stream()
                .filter(r -> r.campus.equalsIgnoreCase("Flinders City Campus"))
                .toList();
        List<ClassRecord> bedfordTonsley = records.stream()
                .filter(r -> !r.campus.equalsIgnoreCase("Flinders City Campus"))
                .toList();

        // City topic formats are kept separate from Bedford Park/Tonsley topic formats.
        // Bedford Park and Tonsley are allowed to mix for the same topic.
        buildOptionsForCampusCluster(cityOnly, combinations, settings);
        buildOptionsForCampusCluster(bedfordTonsley, combinations, settings);

        return combinations.stream()
                .sorted(Comparator.comparingInt(option -> score(option, settings) + conflictPenalty(option, settings)))
                .limit(25)
                .toList();
    }

    private void buildOptionsForCampusCluster(List<ClassRecord> records, List<List<ClassRecord>> combinations, GenerationSettings settings) {
        if (records.isEmpty()) return;
        Map<String, List<ClassRecord>> byClass = records.stream()
                .collect(Collectors.groupingBy(r -> r.className, LinkedHashMap::new, Collectors.toList()));
        List<List<List<ClassRecord>>> choicesPerClass = new ArrayList<>();
        for (Map.Entry<String, List<ClassRecord>> e : byClass.entrySet()) {
            Map<String, List<ClassRecord>> byInstance = e.getValue().stream()
                    .collect(Collectors.groupingBy(ClassRecord::groupKey, LinkedHashMap::new, Collectors.toList()));
            choicesPerClass.add(new ArrayList<>(byInstance.values()));
        }
        combineInstances(choicesPerClass, 0, new ArrayList<>(), combinations, settings);
    }

    private void combineInstances(List<List<List<ClassRecord>>> choices, int index, List<ClassRecord> current, List<List<ClassRecord>> output, GenerationSettings settings) {
        if (index == choices.size()) {
            output.add(new ArrayList<>(current));
            return;
        }
        for (List<ClassRecord> instance : choices.get(index)) {
            current.addAll(instance);
            combineInstances(choices, index + 1, current, output, settings);
            current.subList(current.size() - instance.size(), current.size()).clear();
        }
    }

    private void backtrack(List<List<List<ClassRecord>>> topicOptions, int index, List<ClassRecord> current, GenerationSettings settings, Candidate best) {
        if (index == topicOptions.size()) {
            int total = score(current, settings) + conflictPenalty(current, settings);
            if (total < best.score) { best.score = total; best.records = new ArrayList<>(current); }
            return;
        }
        for (List<ClassRecord> option : topicOptions.get(index)) {
            current.addAll(option);
            backtrack(topicOptions, index + 1, current, settings, best);
            current.subList(current.size() - option.size(), current.size()).clear();
        }
    }

    public List<Conflict> findConflicts(List<ClassRecord> records, boolean allowLectureOverlap) {
        List<Conflict> conflicts = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            for (int j = i + 1; j < records.size(); j++) {
                ClassRecord a = records.get(i), b = records.get(j);
                if (!a.day.equalsIgnoreCase(b.day)) continue;
                if (allowLectureOverlap && (a.isLecture() || b.isLecture())) continue;
                boolean overlap = a.startAsTime().isBefore(b.endAsTime()) && b.startAsTime().isBefore(a.endAsTime());
                if (overlap) conflicts.add(new Conflict(a, b, "Time clash"));
                else {
                    ClassRecord earlier = a.endAsTime().isBefore(b.endAsTime()) ? a : b;
                    ClassRecord later = earlier == a ? b : a;
                    long gap = Duration.between(earlier.endAsTime(), later.startAsTime()).toMinutes();
                    if (gap >= 0 && gap < 30 && !earlier.campus.equalsIgnoreCase(later.campus)) {
                        conflicts.add(new Conflict(earlier, later, "Less than 30 minutes to commute between campuses"));
                    }
                }
            }
        }
        return conflicts;
    }

    private int conflictPenalty(List<ClassRecord> records, GenerationSettings settings) { return findConflicts(records, settings.allowLectureOverlap).size() * 10000; }

    private int score(List<ClassRecord> records, GenerationSettings settings) {
        int score = 0;
        for (int rank = 0; rank < settings.preferences.size(); rank++) {
            String p = settings.preferences.get(rank);
            int weight = (settings.preferences.size() - rank) * 10;
            score -= preferenceHits(records, p) * weight;
        }
        return score;
    }

    private int preferenceHits(List<ClassRecord> records, String preference) {
        String p = preference.toLowerCase(Locale.ROOT);
        if (p.equals("mornings")) return (int) records.stream().filter(r -> r.startAsTime().getHour() < 12).count();
        if (p.equals("afternoons")) return (int) records.stream().filter(r -> r.startAsTime().getHour() >= 12).count();
        if (p.endsWith("s") && List.of("mondays","tuesdays","wednesdays","thursdays","fridays").contains(p)) {
            String day = p.substring(0, 1).toUpperCase(Locale.ROOT) + p.substring(1, p.length() - 1);
            return (int) records.stream().filter(r -> r.day.equalsIgnoreCase(day)).count();
        }
        if (p.equals("bedford park") || p.equals("tonsley") || p.equals("flinders city campus")) return (int) records.stream().filter(r -> r.campus.equalsIgnoreCase(preference)).count();
        if (p.equals("all at the same campus")) return records.stream().map(r -> r.campus).distinct().count() == 1 ? 10 : 0;
        long days = records.stream().map(r -> r.day).distinct().count();
        if (p.equals("evenly spread classes across days")) return (int) days;
        if (p.equals("compact classes to as few days as possible")) return 10 - (int) days;
        return 0;
    }

    private String uniqueName(String base) {
        Set<String> names = store.timetables.stream().map(t -> t.name.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        if (!names.contains(base.toLowerCase(Locale.ROOT))) return base;
        int i = 2;
        while (names.contains((base + " " + i).toLowerCase(Locale.ROOT))) i++;
        return base + " " + i;
    }

    public Optional<Timetable> findByName(String name) { return store.timetables.stream().filter(t -> t.name.equalsIgnoreCase(name)).findFirst(); }
    public boolean deleteByName(String name) { boolean removed = store.timetables.removeIf(t -> t.name.equalsIgnoreCase(name)); if (removed) store.save(); return removed; }
    public List<Timetable> all() { return store.timetables; }
    public GenerationSettings lastSettings() { return store.lastSettings; }
    public void save() { store.save(); }

    public List<ClassRecord> alternativesFor(ClassRecord target) {
        return store.records.stream().filter(r -> r.swapKey().equals(target.swapKey()) && !r.groupKey().equals(target.groupKey())).toList();
    }

    public Path export(Timetable t) throws IOException {
        Path dir = Paths.get("exports");
        Files.createDirectories(dir);
        Path file = dir.resolve(t.name.replaceAll("[^a-zA-Z0-9._-]", "_") + ".csv");
        List<String> lines = new ArrayList<>();
        lines.add("Topic,Availability,Class,Class instance,Date,Day,Time,Location");
        t.classes.stream().sorted(ClassService.classComparator()).forEach(r -> lines.add(r.toCsvRow()));
        Files.write(file, lines, StandardCharsets.UTF_8);
        return file;
    }

    private static class Candidate { int score = Integer.MAX_VALUE; List<ClassRecord> records = new ArrayList<>(); }
}
