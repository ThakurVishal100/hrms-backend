package com.hrms.service;

import com.hrms.dto.BiometricDataDTO;
import com.hrms.dto.EntryExitCdrDTO;
import com.hrms.entity.Attendance;
import com.hrms.entity.Employee;
import com.hrms.entity.EntryExitCdr;
import com.hrms.repository.AttendanceRepository;
import com.hrms.repository.EmployeeRepository;
import com.hrms.repository.EntryExitCdrRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EntryExitCdrRepository cdrRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    // 1. MASTER RECONCILIATION LOGIC
    @Transactional
    public String reconcileAttendance(List<BiometricDataDTO> csvData, Map<String, Map<LocalDate, BiometricDataDTO.TimeEntry>> whatsappData) {
        // 1. Optimize CSV Lookup
        Map<Integer, BiometricDataDTO> bioMap = csvData.stream()
                .filter(dto -> dto.getBiometricCode() != null)
                .collect(Collectors.toMap(
                        BiometricDataDTO::getBiometricCode,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        // 2. Fetch ALL Employees
        List<Employee> allEmployees = employeeRepository.findAll();
        int employeesProcessed = 0;

        System.out.println("--- Starting Reconciliation (Token+Fuzzy Match) ---");

        for (Employee employee : allEmployees) {
            employeesProcessed++;

            // A. Retrieve Excel Data
            BiometricDataDTO bioEntry = null;
            if (employee.getBiometricCode() != null) {
                bioEntry = bioMap.get(employee.getBiometricCode());
            }

            // B. Retrieve WhatsApp Data (ROBUST MATCHING)
            Map<LocalDate, BiometricDataDTO.TimeEntry> empWaLogs = null;
            if (whatsappData != null && employee.getEmployeeName() != null) {
                String dbName = employee.getEmployeeName();

                for (String waName : whatsappData.keySet()) {
                    // Uses Token-Based Fuzzy Matcher
                    if (isNameMatch(dbName, waName)) {
                        System.out.println(">> MATCH SUCCESS: DB['" + dbName + "'] matched WA['" + waName + "']");
                        empWaLogs = whatsappData.get(waName);
                        break;
                    }
                }
            }

            // C. Consolidate Dates
            Set<LocalDate> datesToProcess = new HashSet<>();
            if (bioEntry != null) datesToProcess.addAll(bioEntry.getDailyRecords().keySet());
            if (empWaLogs != null) datesToProcess.addAll(empWaLogs.keySet());

            // D. Process Each Date
            for (LocalDate date : datesToProcess) {
                Attendance attendance = attendanceRepository.findDailyAttendance(employee.getEmployeeId(), date)
                        .orElse(new Attendance());

                if (attendance.getAttId() == null) {
                    attendance.setEmployee(employee);
                    attendance.setCompany(employee.getCompany());
                    attendance.setAttendanceDate(date);
                }

                // 1. POPULATE RAW DATA
                // Excel
                if (bioEntry != null && bioEntry.getDailyRecords().containsKey(date)) {
                    BiometricDataDTO.TimeEntry bioTime = bioEntry.getDailyRecords().get(date);
                    attendance.setBioInTime(bioTime.getInTime());
                    attendance.setBioOutTime(bioTime.getOutTime());
                } else {
                    attendance.setBioInTime(null);
                    attendance.setBioOutTime(null);
                }

                // WhatsApp
                attendance.setWhatsappInTime(null);
                attendance.setWhatsappOutTime(null);
                if (empWaLogs != null && empWaLogs.containsKey(date)) {
                    attendance.setWhatsappInTime(empWaLogs.get(date).getInTime());
                    attendance.setWhatsappOutTime(empWaLogs.get(date).getOutTime());
                }

                // 2. DETERMINE FINAL TIMES (Priority: Bio > Web > WhatsApp)
                LocalTime finalIn = null;
                LocalTime finalOut = null;

                if (attendance.getBioInTime() != null) {
                    finalIn = attendance.getBioInTime();
                    finalOut = attendance.getBioOutTime();
                } else if (attendance.getWebInTime() != null) {
                    finalIn = attendance.getWebInTime();
                    finalOut = attendance.getWebOutTime();
                } else if (attendance.getWhatsappInTime() != null) {
                    finalIn = attendance.getWhatsappInTime();
                    finalOut = attendance.getWhatsappOutTime();
                }

                // 3. SET STATUS & CALCULATE HOURS
                if (finalIn != null) {
                    attendance.setStatus("Present");
                    attendance.setFirstInTime(LocalDateTime.of(date, finalIn));

                    if (finalOut != null) {
                        attendance.setLastOutTime(LocalDateTime.of(date, finalOut));
                        long seconds = Duration.between(finalIn, finalOut).getSeconds();
                        attendance.setEffectiveWorkingHours(convertToDisplayHours(seconds));
                    } else {
                        attendance.setEffectiveWorkingHours(0.0);
                    }
                } else {
                    DayOfWeek dayOfWeek = date.getDayOfWeek();
                    if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                        attendance.setStatus("Week Off");
                    } else {
                        attendance.setStatus("Absent");
                    }
                    attendance.setEffectiveWorkingHours(0.0);
                }

                attendanceRepository.save(attendance);
            }
        }
        return "Sync Complete. Processed: " + employeesProcessed;
    }

    // HELPER: ROBUST TOKEN & FUZZY MATCHING
    private boolean isNameMatch(String dbNameRaw, String waNameRaw) {
        if (dbNameRaw == null || waNameRaw == null) return false;

        // 1. Clean strings (Keep spaces for tokenization)
        String db = dbNameRaw.toLowerCase().replaceAll("[^a-z0-9\\s]", "").trim();
        String wa = waNameRaw.toLowerCase().replaceAll("[^a-z0-9\\s]", "").trim();

        // 2. Exact Match
        if (db.equals(wa)) return true;


        String[] dbTokens = db.split("\\s+");
        String[] waTokens = wa.split("\\s+");

        // 4. Cross-Check Tokens
        for (String wToken : waTokens) {
            if (wToken.length() < 3) continue; // Skip short words like "mr", "dr"

            for (String dToken : dbTokens) {
                // Exact Token Match (Aakash == Aakash)
                if (wToken.equals(dToken)) return true;

                // Fuzzy Match (Akash ~= Aakash) | Edit Distance <= 1
                if (Math.abs(wToken.length() - dToken.length()) <= 1) {
                    if (computeLevenshteinDistance(wToken, dToken) <= 1) return true;
                }
            }
        }
        return false;
    }

    // Calculates how many edits (insert/delete/sub) needed to change s1 to s2
    private int computeLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) dp[i][j] = j;
                else if (j == 0) dp[i][j] = i;
                else {
                    dp[i][j] = min(
                            dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1),
                            dp[i - 1][j] + 1,
                            dp[i][j - 1] + 1
                    );
                }
            }
        }
        return dp[s1.length()][s2.length()];
    }

    private int min(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

    // 2. WEB PUNCH METHODS
    @Transactional
    public String punchIn(Integer employeeId, String location) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        Optional<EntryExitCdr> staleSession = cdrRepository.findStaleSession(employeeId, today);
        if (staleSession.isPresent()) {
            EntryExitCdr stale = staleSession.get();
            LocalDateTime autoOutTime = stale.getInTime().plusHours(9);
            stale.setOutTime(autoOutTime);
            stale.setOutLocation("Auto-Closed (System Default)");
            stale.setUpdateTimeOutTimeEntry(LocalDateTime.now());
            cdrRepository.save(stale);
            updateWebAttendance(employeeId, stale.getCdrDate(), null, autoOutTime);
        }

        Optional<EntryExitCdr> openSession = cdrRepository.findOpenSession(employeeId, today);
        if (openSession.isPresent()) {
            return "Already Checked In!";
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        EntryExitCdr newCdr = new EntryExitCdr();
        newCdr.setEmployee(employee);
        newCdr.setCompany(employee.getCompany());
        newCdr.setCdrDate(today);
        newCdr.setInTime(now);
        newCdr.setInLocation(location);
        newCdr.setUpdateTimeInTimeEntry(now);
        cdrRepository.save(newCdr);

        updateWebAttendance(employeeId, today, now, null);

        return "Checked In Successfully";
    }

    @Transactional
    public String punchOut(Integer employeeId, String location) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        Optional<EntryExitCdr> openSessionOpt = cdrRepository.findOpenSession(employeeId, today);
        if (openSessionOpt.isEmpty()) {
            openSessionOpt = cdrRepository.findOpenSession(employeeId, today.minusDays(1));
        }

        EntryExitCdr currentCdr = openSessionOpt
                .orElseThrow(() -> new RuntimeException("You are not checked in!"));

        currentCdr.setOutTime(now);
        currentCdr.setOutLocation(location);
        currentCdr.setUpdateTimeOutTimeEntry(now);
        cdrRepository.saveAndFlush(currentCdr);

        updateWebAttendance(employeeId, currentCdr.getCdrDate(), null, now);

        long totalSeconds = Duration.between(currentCdr.getInTime(), now).getSeconds();
        return "Checked Out. Session Duration: " + formatSecondsToTime(totalSeconds);
    }

    private void updateWebAttendance(Integer employeeId, LocalDate date, LocalDateTime inTime, LocalDateTime outTime) {
        Attendance attendance = attendanceRepository.findDailyAttendance(employeeId, date)
                .orElse(new Attendance());

        if (attendance.getAttId() == null) {
            Employee emp = employeeRepository.findById(employeeId).orElse(null);
            attendance.setEmployee(emp);
            if (emp != null) attendance.setCompany(emp.getCompany());
            attendance.setAttendanceDate(date);
            attendance.setStatus("Present");
        }

        if (inTime != null) {
            if (attendance.getWebInTime() == null) {
                attendance.setWebInTime(inTime.toLocalTime());
            }
            if (attendance.getFirstInTime() == null) {
                attendance.setFirstInTime(inTime);
            }
        }

        if (outTime != null) {
            attendance.setWebOutTime(outTime.toLocalTime());
            attendance.setLastOutTime(outTime);
        }

        if (attendance.getFirstInTime() != null && attendance.getLastOutTime() != null) {
            long seconds = Duration.between(attendance.getFirstInTime(), attendance.getLastOutTime()).getSeconds();
            attendance.setEffectiveWorkingHours(convertToDisplayHours(seconds));
            attendance.setStatus("Present");
        }

        attendanceRepository.save(attendance);
    }

    // 3. UTILITY METHODS

    private Double convertToDisplayHours(long totalSeconds) {
        if (totalSeconds < 0) totalSeconds = 0;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        return hours + (minutes / 100.0);
    }

    public List<EntryExitCdrDTO> getLast30DaysLogs(Integer empId) {
        List<EntryExitCdr> entities = cdrRepository.findByEmployee_EmployeeIdOrderByCdrDateDesc(empId);
        if (entities == null) return Collections.emptyList();
        return entities.stream().limit(30).map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<EntryExitCdrDTO> getRawLogsForMonth(Integer empId, int month, int year) {
        List<EntryExitCdr> entities = cdrRepository.findRawLogsByMonth(empId, month, year);
        return entities.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public String getCurrentStatus(Integer employeeId) {
        Optional<EntryExitCdr> today = cdrRepository.findOpenSession(employeeId, LocalDate.now());
        if (today.isPresent()) return "IN";
        Optional<EntryExitCdr> yesterday = cdrRepository.findOpenSession(employeeId, LocalDate.now().minusDays(1));
        if (yesterday.isPresent()) return "IN";
        return "OUT";
    }

    private EntryExitCdrDTO convertToDTO(EntryExitCdr entity) {
        EntryExitCdrDTO dto = new EntryExitCdrDTO();
        dto.setCdrId(entity.getCdrId());
        dto.setCdrDate(entity.getCdrDate());
        dto.setInTime(entity.getInTime());
        dto.setOutTime(entity.getOutTime());
        dto.setInLocation(entity.getInLocation());
        dto.setOutLocation(entity.getOutLocation());
        if (entity.getInTime() != null && entity.getOutTime() != null) {
            long seconds = Duration.between(entity.getInTime(), entity.getOutTime()).getSeconds();
            dto.setDuration(formatSecondsToTime(seconds));
        }
        return dto;
    }

    private String formatSecondsToTime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}