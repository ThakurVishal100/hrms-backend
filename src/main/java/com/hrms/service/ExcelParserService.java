package com.hrms.service;

import com.hrms.dto.BiometricDataDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ExcelParserService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");

    // CSV/Excel Parser (Existing Logic)
    public List<BiometricDataDTO> parseBiometricFile(MultipartFile file) {
        List<BiometricDataDTO> biometricList = new ArrayList<>();
        System.out.println("--- STARTING PARSE ---");

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            int currentYear = LocalDate.now().getYear();
            int currentMonth = LocalDate.now().getMonthValue();
            BiometricDataDTO currentEmp = null;
            boolean expectingEmpDataRow = false;
            Map<Integer, Integer> columnToDayMap = new HashMap<>();

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] columns = line.split(",", -1);
                for(int i=0; i<columns.length; i++) columns[i] = columns[i].replace("\"", "").trim();

                if (line.contains("Report Date From")) {
                    int[] ym = extractYearMonth(line);
                    if (ym != null) { currentYear = ym[0]; currentMonth = ym[1]; }
                }

                boolean foundLabel = false;
                for (String col : columns) if ("EmpCode".equalsIgnoreCase(col)) { foundLabel = true; break; }

                if (foundLabel) {
                    if (currentEmp != null) biometricList.add(currentEmp);
                    currentEmp = new BiometricDataDTO();
                    expectingEmpDataRow = true;
                    columnToDayMap.clear();
                    continue;
                }

                if (expectingEmpDataRow) {
                    for (int i = 0; i < columns.length; i++) {
                        if (isNumeric(columns[i])) {
                            currentEmp.setBiometricCode(Integer.parseInt(columns[i]));
                            for (int j = i + 1; j < columns.length; j++) {
                                if (!columns[j].isEmpty() && !isNumeric(columns[j])) {
                                    currentEmp.setEmployeeName(columns[j]);
                                    break;
                                }
                            }
                            break;
                        }
                    }
                    expectingEmpDataRow = false;
                    continue;
                }

                if (isDateRow(columns)) {
                    columnToDayMap = mapColumnsToDays(columns);
                    continue;
                }
                if (currentEmp != null) {
                    if (containsKeyword(columns, "Arrived")) {
                        parseTimeRow(currentEmp, columns, columnToDayMap, currentYear, currentMonth, true);
                    }
                    if (containsKeyword(columns, "Depart") || containsKeyword(columns, "Dept")) {
                        parseTimeRow(currentEmp, columns, columnToDayMap, currentYear, currentMonth, false);
                    }
                }
            }
            if (currentEmp != null) biometricList.add(currentEmp);

        } catch (Exception e) {
            throw new RuntimeException("Parse Error: " + e.getMessage());
        }
        return biometricList;
    }

    // --- HELPERS ---
    private String extractValue(String line, String regex) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(line);
        return m.find() ? m.group(1).trim() : null;
    }
    private int[] extractYearMonth(String line) {
        Pattern p = Pattern.compile("(\\d{2})-(\\d{2})-(\\d{4})");
        Matcher m = p.matcher(line);
        return m.find() ? new int[]{Integer.parseInt(m.group(3)), Integer.parseInt(m.group(2))} : null;
    }
    private boolean isNumeric(String str) { return str != null && str.matches("\\d+"); }
    private boolean isDateRow(String[] columns) {
        int c = 0; for (String col : columns) if ("01".equals(col) || "1".equals(col)) c++;
        return c >= 1;
    }
    private Map<Integer, Integer> mapColumnsToDays(String[] columns) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < columns.length; i++) if (isNumeric(columns[i])) map.put(i, Integer.parseInt(columns[i]));
        return map;
    }
    private boolean containsKeyword(String[] columns, String keyword) {
        for(String col : columns) if(col.toLowerCase().contains(keyword.toLowerCase())) return true;
        return false;
    }
    private void parseTimeRow(BiometricDataDTO emp, String[] columns, Map<Integer, Integer> dayMap, int year, int month, boolean isIn) {
        if (dayMap == null) return;
        for (Map.Entry<Integer, Integer> entry : dayMap.entrySet()) {
            int col = entry.getKey();
            try {
                LocalDate date = LocalDate.of(year, month, entry.getValue());
                emp.getDailyRecords().putIfAbsent(date, new BiometricDataDTO.TimeEntry());
                if (col < columns.length && isValidTime(columns[col])) {
                    if (isIn) emp.getDailyRecords().get(date).setInTime(LocalTime.parse(columns[col], TIME_FORMATTER));
                    else emp.getDailyRecords().get(date).setOutTime(LocalTime.parse(columns[col], TIME_FORMATTER));
                }
            } catch (Exception e) {}
        }
    }
    private boolean isValidTime(String s) {
        return s != null && !s.isEmpty() && !s.equals("00:00") && s.matches("\\d{1,2}:\\d{2}");
    }

    public Map<String, Map<LocalDate, BiometricDataDTO.TimeEntry>> parseWhatsAppText(String text) {
        Map<String, Map<LocalDate, BiometricDataDTO.TimeEntry>> whatsappData = new HashMap<>();
        if (text == null || text.trim().isEmpty()) return whatsappData;

        String[] lines = text.split("\\n");
        LocalDate currentDate = null;

        Pattern datePattern = Pattern.compile("(\\d{2})-(\\d{2})-(\\d{4})");

        // Matches "09 : 25 Am", "8:00AM", "10:30" (optional AM/PM)
        Pattern timePattern = Pattern.compile("(\\d{1,2}\\s*:\\s*\\d{2})(?:\\s*([aApP][mM]))?");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // 1. Check Date Header
            Matcher dateMatcher = datePattern.matcher(line);
            if (dateMatcher.find()) {
                currentDate = LocalDate.of(Integer.parseInt(dateMatcher.group(3)), Integer.parseInt(dateMatcher.group(2)), Integer.parseInt(dateMatcher.group(1)));
                continue;
            }

            // 2. Parse Name and Times
            if (currentDate != null) {
                // Get Name (Before first digit)
                int firstDigitIdx = -1;
                for (int i = 0; i < line.length(); i++) {
                    if (Character.isDigit(line.charAt(i))) { firstDigitIdx = i; break; }
                }

                if (firstDigitIdx != -1) {
                    String name = line.substring(0, firstDigitIdx).replaceAll("[:\\-]", "").trim().toLowerCase();
                    Matcher timeMatcher = timePattern.matcher(line);
                    List<LocalTime> times = new ArrayList<>();

                    while (timeMatcher.find()) {
                        String rawTime = timeMatcher.group(1); // 09 : 25
                        String ampm = timeMatcher.group(2);    // Am (nullable)

                        LocalTime t = parseFlexibleTime(rawTime, ampm, times.isEmpty()); // isEmpty=true means IN time
                        if(t != null) times.add(t);
                    }

                    if (!name.isEmpty() && !times.isEmpty()) {
                        BiometricDataDTO.TimeEntry entry = new BiometricDataDTO.TimeEntry();
                        entry.setInTime(times.get(0));
                        if (times.size() > 1) entry.setOutTime(times.get(times.size() - 1));

                        whatsappData.computeIfAbsent(name, k -> new HashMap<>()).put(currentDate, entry);
                    }
                }
            }
        }
        return whatsappData;
    }

    private LocalTime parseFlexibleTime(String timePart, String ampm, boolean isFirst) {
        try {
            String clean = timePart.replaceAll("\\s+", ""); // 09:25
            LocalTime t = LocalTime.parse(clean, DateTimeFormatter.ofPattern("H:mm"));

            // Handle AM/PM Logic
            if (ampm != null) {
                if (ampm.toLowerCase().contains("p") && t.getHour() < 12) t = t.plusHours(12);
                if (ampm.toLowerCase().contains("a") && t.getHour() == 12) t = t.minusHours(12);
            } else {
                // Heuristic for missing AM/PM: If Out time is small (e.g. 6:45), assume PM
                if (!isFirst && t.getHour() < 9) t = t.plusHours(12);
            }
            return t;
        } catch (Exception e) { return null; }
    }
}