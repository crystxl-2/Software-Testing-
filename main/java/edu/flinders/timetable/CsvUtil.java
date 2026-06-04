package edu.flinders.timetable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class CsvUtil {
    public static List<Map<String, String>> readCsv(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty()) throw new IllegalArgumentException("CSV file is empty.");

        List<String> headers = parseLine(lines.get(0)).stream()
                .map(CsvUtil::normaliseHeader)
                .toList();

        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).trim().isEmpty()) continue;
            List<String> values = parseLine(lines.get(i));

            // Some of the supplied sample files leave the final location/room value unquoted
            // even when it contains commas. Treat any surplus comma-separated values as part
            // of the final column instead of rejecting an otherwise valid row.
            if (values.size() > headers.size()) values = mergeTrailingValues(values, headers.size());

            if (values.size() != headers.size()) {
                throw new IllegalArgumentException("CSV row " + (i + 1) + " has " + values.size() + " values but expected " + headers.size() + ".");
            }
            Map<String, String> row = new LinkedHashMap<>();
            for (int j = 0; j < headers.size(); j++) row.put(headers.get(j), values.get(j).trim());
            rows.add(row);
        }
        return rows;
    }

    private static String normaliseHeader(String header) {
        if (header == null) return "";
        return header.replace("﻿", "").trim();
    }

    private static List<String> mergeTrailingValues(List<String> values, int expectedSize) {
        if (expectedSize < 1) return values;
        List<String> merged = new ArrayList<>(values.subList(0, expectedSize - 1));
        merged.add(String.join(",", values.subList(expectedSize - 1, values.size())));
        return merged;
    }

    public static List<String> parseLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else current.append(c);
        }
        result.add(current.toString());
        return result;
    }

    public static String escape(String s) {
        if (s == null) return "";
        boolean needsQuotes = s.contains(",") || s.contains("\"") || s.contains("\n");
        String escaped = s.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }
}
