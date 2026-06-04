package edu.flinders.timetable;

import java.nio.file.Path;
import java.util.*;

public class ConsoleUI {
    private final Scanner sc = new Scanner(System.in);
    private final DataStore store = DataStore.load();
    private final ClassService classService = new ClassService(store);
    private final TimetableService timetableService = new TimetableService(store);
    private final String[] fields = {"topicCode","topicName","attendanceMode","campus","semester","availabilityNumber","className","classInstance","firstClassDate","lastClassDate","day","startTime","endTime","building","room"};
    private final List<String> prefs = List.of("Bedford Park", "Tonsley", "Flinders City Campus", "all at the same campus", "mornings", "afternoons", "Mondays", "Tuesdays", "Wednesdays", "Thursdays", "Fridays", "evenly spread classes across days", "compact classes to as few days as possible");

    public void run() {
        title();
        while (true) {
            menu();
            String choice = ask("Choose option");
            try {
                switch (choice) {
                    case "1" -> importClasses();
                    case "2" -> browseClasses();
                    case "3" -> viewClasses();
                    case "4" -> searchClasses();
                    case "5" -> editClass();
                    case "6" -> deleteClass();
                    case "7" -> generateTimetable();
                    case "8" -> browseTimetables();
                    case "9" -> viewTimetable();
                    case "10" -> editTimetable();
                    case "11" -> deleteTimetable();
                    case "12" -> exportTimetable();
                    case "0" -> { println(Ansi.GREEN, "Goodbye."); return; }
                    default -> println(Ansi.RED, "Invalid option.");
                }
            } catch (Exception e) { println(Ansi.RED, "Error: " + e.getMessage()); }
        }
    }

    private void title() {
        System.out.println(Ansi.CYAN + Ansi.BOLD + """
==============================================
   FLINDERS STUDENT TIMETABLE OPTIMIZER
==============================================
        _____   _____   ____
       / ___/  /_  _/  / __/
       | |       / /   / /_
       | |___   / /   / __/
       |____/  /_/   /_/
""" + Ansi.RESET);
        System.out.println(Ansi.ITALIC + "Student Timetable Optimizer" + Ansi.RESET);
    }

    private void menu() {
        System.out.println(Ansi.BOLD + "\nMain Menu" + Ansi.RESET);
        System.out.println("1. Import classes from CSV");
        System.out.println("2. Browse classes");
        System.out.println("3. View classes");
        System.out.println("4. Search classes");
        System.out.println("5. Edit class");
        System.out.println("6. Delete class");
        System.out.println("7. Generate timetable");
        System.out.println("8. Browse timetables");
        System.out.println("9. View timetable");
        System.out.println("10. Edit timetable");
        System.out.println("11. Delete timetable");
        System.out.println("12. Export timetable");
        System.out.println("0. Exit");
    }

    private void importClasses() throws Exception {
        String path = ask("CSV file path");
        ImportResult result = classService.importCsv(path);
        println(Ansi.GREEN, "Import complete. New records: " + result.created + ". Updated records: " + result.updated + ".");
    }

    private void browseClasses() {
        Map<String, List<ClassRecord>> groups = classService.grouped(classService.search(Map.of()));
        int i = 1;
        for (List<ClassRecord> group : groups.values()) {
            ClassRecord r = group.get(0);
            System.out.printf("%3d. %s | %s | %s #%s | %d date row(s)%n", i++, r.topicFull(), r.availabilityFull(), r.className, r.classInstance, group.size());
        }
        if (groups.isEmpty()) println(Ansi.YELLOW, "No class data imported yet.");
    }

    private void viewClasses() { printRecords(classService.search(Map.of())); }

    private void searchClasses() {
        Map<String, String> criteria = collectCriteria();
        printRecords(classService.search(criteria));
    }

    private void editClass() {
        List<ClassRecord> records = classService.search(Map.of());
        printRecordsWithIds(records);
        String id = ask("Enter record ID to edit");
        ClassRecord r = classService.findById(id).orElseThrow(() -> new IllegalArgumentException("Record not found."));
        System.out.println("Editing: " + r.detailedLine());
        printFields();
        String field = ask("Field to change");
        if (!List.of(fields).contains(field)) throw new IllegalArgumentException("Invalid field.");
        String value = ask("New value");
        warn("This will permanently update the selected class record.");
        if (confirm()) {
            r.setField(field, value);
            classService.save();
            println(Ansi.GREEN, "Class updated.");
        } else println(Ansi.YELLOW, "Edit cancelled.");
    }

    private void deleteClass() {
        printRecordsWithIds(classService.search(Map.of()));
        String id = ask("Enter record ID to delete");
        warn("This will delete the class record and remove it from saved timetables.");
        if (confirm() && classService.deleteById(id)) println(Ansi.GREEN, "Class deleted."); else println(Ansi.YELLOW, "Delete cancelled or record not found.");
    }

    private void generateTimetable() {
        GenerationSettings s = new GenerationSettings();
        GenerationSettings previous = timetableService.lastSettings();
        String previousName = previous.timetableName == null ? "" : previous.timetableName;
        s.timetableName = ask("Timetable name (blank = auto generated) [last: " + previousName + "]");
        s.semesters = askSet("Semester: enter 1, 2, or both separated by comma", Set.of("1","2"));
        s.topics = selectTopics();
        s.campuses = askSet("Campus: Bedford Park, Tonsley, Flinders City Campus. Separate by comma", Set.of("Bedford Park","Tonsley","Flinders City Campus"));
        s.allowLectureOverlap = ask("Allow lecture overlap? yes/no").equalsIgnoreCase("yes");
        s.preferences = selectPreferences();
        Timetable t = timetableService.generate(s);
        println(Ansi.GREEN, "Generated timetable: " + t.name);
        displayTimetable(t);
    }

    private void browseTimetables() {
        if (timetableService.all().isEmpty()) { println(Ansi.YELLOW, "No saved timetables."); return; }
        int i = 1;
        for (Timetable t : timetableService.all()) System.out.printf("%3d. %s | %d class row(s) | created %s%n", i++, t.name, t.classes.size(), t.createdAt);
    }

    private void viewTimetable() {
        Timetable t = pickTimetable();
        displayTimetable(t);
    }

    private void editTimetable() {
        Timetable t = pickTimetable();
        displayTimetable(t);
        String id = ask("Enter class record ID in this timetable to swap");
        ClassRecord target = t.classes.stream().filter(r -> r.id.equals(id)).findFirst().orElseThrow(() -> new IllegalArgumentException("Record not in timetable."));
        List<ClassRecord> alternatives = timetableService.alternativesFor(target);
        if (alternatives.isEmpty()) throw new IllegalArgumentException("No alternative class instance found for the same topic and class.");
        printRecordsWithIds(alternatives);
        String replacementId = ask("Enter replacement record ID");
        ClassRecord replacement = alternatives.stream().filter(r -> r.id.equals(replacementId)).findFirst().orElseThrow(() -> new IllegalArgumentException("Replacement not found."));
        List<ClassRecord> proposed = new ArrayList<>(t.classes);
        proposed.removeIf(r -> r.groupKey().equals(target.groupKey()));
        proposed.addAll(store.records.stream().filter(r -> r.groupKey().equals(replacement.groupKey())).toList());
        List<Conflict> conflicts = timetableService.findConflicts(proposed, false);
        if (!conflicts.isEmpty()) { warn("This swap creates a clash or commute warning."); printConflicts(conflicts); if (!confirm()) { println(Ansi.YELLOW, "Swap cancelled."); return; } }
        t.classes = proposed.stream().sorted(ClassService.classComparator()).toList();
        timetableService.save();
        println(Ansi.GREEN, "Timetable updated.");
    }

    private void deleteTimetable() {
        Timetable t = pickTimetable();
        warn("This will permanently delete timetable: " + t.name);
        if (confirm() && timetableService.deleteByName(t.name)) println(Ansi.GREEN, "Timetable deleted."); else println(Ansi.YELLOW, "Delete cancelled.");
    }

    private void exportTimetable() throws Exception {
        Timetable t = pickTimetable();
        Path file = timetableService.export(t);
        println(Ansi.GREEN, "Exported to: " + file.toAbsolutePath());
    }

    private Map<String, String> collectCriteria() {
        System.out.println("Enter search criteria. Leave blank to ignore a field.");
        printFields();
        Map<String, String> c = new LinkedHashMap<>();
        for (String f : fields) {
            String v = ask(f);
            if (!v.isBlank()) c.put(f, v);
        }
        return c;
    }

    private Set<String> selectTopics() {
        List<String> topics = store.records.stream().map(ClassRecord::topicFull).distinct().sorted().toList();
        if (topics.isEmpty()) throw new IllegalArgumentException("Import class data before generating a timetable.");
        System.out.println("Available topics:");
        for (int i = 0; i < topics.size(); i++) System.out.println((i + 1) + ". " + topics.get(i));
        String input = ask("Select topic numbers separated by comma");
        Set<String> selected = new LinkedHashSet<>();
        for (String part : input.split(",")) {
            if (part.trim().isEmpty()) continue;
            int idx = Integer.parseInt(part.trim()) - 1;
            if (idx < 0 || idx >= topics.size()) throw new IllegalArgumentException("Invalid topic number: " + part);
            selected.add(topics.get(idx));
        }
        if (selected.isEmpty()) throw new IllegalArgumentException("Selecting no topics is invalid.");
        return selected;
    }

    private List<String> selectPreferences() {
        System.out.println("Preferences. Enter numbers in highest to lowest preference order, separated by comma. Blank = no preferences.");
        for (int i = 0; i < prefs.size(); i++) System.out.println((i + 1) + ". " + prefs.get(i));
        String input = ask("Preference numbers");
        List<String> selected = new ArrayList<>();
        if (input.isBlank()) return selected;
        for (String part : input.split(",")) {
            int idx = Integer.parseInt(part.trim()) - 1;
            if (idx < 0 || idx >= prefs.size()) throw new IllegalArgumentException("Invalid preference number: " + part);
            selected.add(prefs.get(idx));
        }
        return selected;
    }

    private Set<String> askSet(String prompt, Set<String> allowed) {
        String input = ask(prompt + " (blank = all)");
        if (input.isBlank() || input.equalsIgnoreCase("both")) return new LinkedHashSet<>(allowed);
        Set<String> selected = new LinkedHashSet<>();
        for (String part : input.split(",")) {
            String value = part.trim();
            Optional<String> match = allowed.stream().filter(a -> a.equalsIgnoreCase(value)).findFirst();
            selected.add(match.orElseThrow(() -> new IllegalArgumentException("Invalid option: " + value)));
        }
        return selected;
    }

    private Timetable pickTimetable() {
        browseTimetables();
        String name = ask("Timetable name");
        return timetableService.findByName(name).orElseThrow(() -> new IllegalArgumentException("Timetable not found."));
    }

    private void displayTimetable(Timetable t) {
        System.out.println(Ansi.BOLD + Ansi.UNDERLINE + "\n" + t.name + Ansi.RESET);
        printRecordsWithIds(t.classes.stream().sorted(ClassService.classComparator()).toList());
        List<Conflict> conflicts = timetableService.findConflicts(t.classes, false);
        if (!conflicts.isEmpty()) { warn("Warnings detected:"); printConflicts(conflicts); }
    }

    private void printConflicts(List<Conflict> conflicts) {
        for (Conflict c : conflicts) System.out.println(Ansi.YELLOW + "- " + c.message + ": " + c.first.shortLine() + " <-> " + c.second.shortLine() + Ansi.RESET);
    }

    private void printRecords(List<ClassRecord> records) { printRecordsWithIds(records); }

    private void printRecordsWithIds(List<ClassRecord> records) {
        if (records.isEmpty()) { println(Ansi.YELLOW, "No records found."); return; }
        for (ClassRecord r : records) {
            System.out.println(Ansi.CYAN + "ID: " + r.id + Ansi.RESET);
            System.out.println("  " + r.detailedLine());
        }
    }

    private void printFields() { System.out.println("Allowed fields: " + String.join(", ", fields)); }
    private void warn(String msg) { println(Ansi.YELLOW, "WARNING: " + msg); }
    private boolean confirm() { return ask("Type YES to confirm").equals("YES"); }
    private String ask(String prompt) { System.out.print(Ansi.BOLD + prompt + ": " + Ansi.RESET); return sc.nextLine().trim(); }
    private void println(String colour, String msg) { System.out.println(colour + msg + Ansi.RESET); }
}
