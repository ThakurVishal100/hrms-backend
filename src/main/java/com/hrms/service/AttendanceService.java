package com.hrms.service;

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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EntryExitCdrRepository cdrRepository;

    @Autowired
    private EmployeeRepository employeeRepository;


    @Transactional
    public String punchIn(Integer employeeId, String location) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        // 1. EDGE CASE: Handle ANY Stale Session (Yesterday, Friday, etc.)
        Optional<EntryExitCdr> staleSession = cdrRepository.findStaleSession(employeeId, today);

        if (staleSession.isPresent()) {
            EntryExitCdr stale = staleSession.get();

            // A. Auto-close the stale log (Default to 9 Hours shift)
            LocalDateTime autoOutTime = stale.getInTime().plusHours(9);
            stale.setOutTime(autoOutTime);
            stale.setOutLocation("Auto-Closed (System Default)");
            stale.setUpdateTimeOutTimeEntry(LocalDateTime.now());
            cdrRepository.save(stale);

            // B. CRITICAL: Recalculate Summary for that PAST date
            recalculateDailySummary(employeeId, stale.getCdrDate());
        }

        // 2. Standard Check: Already checked in TODAY?
        Optional<EntryExitCdr> openSession = cdrRepository.findOpenSession(employeeId, today);
        if (openSession.isPresent()) {
            return "Already Checked In!";
        }

        // 3. Create NEW Session
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

        // 4. Update Today's Summary (First In Time)
        updateDailySummary(employee, today, now, false);

        return "Checked In Successfully";
    }


    @Transactional
    public String punchOut(Integer employeeId, String location) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // 1. Find Session (Check Today first, then Yesterday for night shifts)
        Optional<EntryExitCdr> openSessionOpt = cdrRepository.findOpenSession(employeeId, today);
        if (openSessionOpt.isEmpty()) {
            openSessionOpt = cdrRepository.findOpenSession(employeeId, today.minusDays(1));
        }

        EntryExitCdr currentCdr = openSessionOpt
                .orElseThrow(() -> new RuntimeException("You are not checked in!"));

        // 2. Close Session
        currentCdr.setOutTime(now);
        currentCdr.setOutLocation(location);
        currentCdr.setUpdateTimeOutTimeEntry(now);
        cdrRepository.saveAndFlush(currentCdr);

        // 3. Recalculate Total Hours for the specific date of the CDR
        long totalSeconds = recalculateDailySummary(employeeId, currentCdr.getCdrDate());

        // 4. Update "Last Out Time" specifically
        updateLastOutTime(employeeId, currentCdr.getCdrDate(), now);

        return "Checked Out. Total Working Hours: " + formatSecondsToTime(totalSeconds);
    }

    // ===================================================================================
    // 2. READ METHODS (FOR DASHBOARD & REPORTS)
    // ===================================================================================

    /**
     * Used by Dashboard to show recent activity history.
     */
    public List<EntryExitCdrDTO> getLast30DaysLogs(Integer empId) {
        // You might need to add this method to EntryExitCdrRepository if not exists:
        // List<EntryExitCdr> findByEmployee_EmployeeIdOrderByCdrDateDesc(Integer empId);
        // OR use a native query if you prefer.
        List<EntryExitCdr> entities = cdrRepository.findByEmployee_EmployeeIdOrderByCdrDateDesc(empId);
        if (entities == null) return Collections.emptyList();

        return entities.stream()
                .limit(30) // Limit to 30 in memory or use Pageable in Repo
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Used by "Daily Report" view in Attendance.jsx
     */
    public List<EntryExitCdrDTO> getRawLogsForMonth(Integer empId, int month, int year) {
        // Requires custom query in Repository: SELECT * ... WHERE month(...) = :month
        List<EntryExitCdr> entities = cdrRepository.findRawLogsByMonth(empId, month, year);
        return entities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Used by Dashboard to toggle "Check In" / "Check Out" button.
     */
    public String getCurrentStatus(Integer employeeId) {
        // Check Today
        Optional<EntryExitCdr> today = cdrRepository.findOpenSession(employeeId, LocalDate.now());
        if(today.isPresent()) return "IN";

        // Check Yesterday (Night shift case)
        Optional<EntryExitCdr> yesterday = cdrRepository.findOpenSession(employeeId, LocalDate.now().minusDays(1));
        if(yesterday.isPresent()) return "IN";

        return "OUT";
    }


    // Converts Entity to DTO to hide internal structure
    private EntryExitCdrDTO convertToDTO(EntryExitCdr entity) {
        EntryExitCdrDTO dto = new EntryExitCdrDTO();
        dto.setCdrId(entity.getCdrId());
        dto.setCdrDate(entity.getCdrDate());
        dto.setInTime(entity.getInTime());
        dto.setOutTime(entity.getOutTime());
        dto.setInLocation(entity.getInLocation());
        dto.setOutLocation(entity.getOutLocation());

        // Calculate duration for this specific log if complete
        if (entity.getInTime() != null && entity.getOutTime() != null) {
            long seconds = Duration.between(entity.getInTime(), entity.getOutTime()).getSeconds();
            dto.setDuration(formatSecondsToTime(seconds)); // Ensure DTO has this field or remove
        }
        return dto;
    }

    // Creates or Updates the 'Attendance' summary row (Single row per day)
    private void updateDailySummary(Employee employee, LocalDate date, LocalDateTime time, boolean isCheckout) {
        Attendance attendance = attendanceRepository.findDailyAttendance(employee.getEmployeeId(), date)
                .orElse(new Attendance());

        if (attendance.getAttId() == null) {
            attendance.setEmployee(employee);
            attendance.setCompany(employee.getCompany());
            attendance.setAttendanceDate(date);
            attendance.setFirstInTime(time); // Set First In
            attendance.setEffectiveWorkingHours(0.0);
        }
        if (isCheckout) {
            attendance.setLastOutTime(time);
        }
        attendanceRepository.save(attendance);
    }

    // Sums up ALL sessions for a specific day and updates the 'Attendance' table
    private long recalculateDailySummary(Integer employeeId, LocalDate date) {
        List<EntryExitCdr> sessions = cdrRepository.findTodaysCdrs(employeeId, date);
        long totalSeconds = 0;

        for (EntryExitCdr session : sessions) {
            if (session.getInTime() != null && session.getOutTime() != null) {
                totalSeconds += Duration.between(session.getInTime(), session.getOutTime()).getSeconds();
            }
        }

        Attendance attendance = attendanceRepository.findDailyAttendance(employeeId, date)
                .orElseThrow(() -> new RuntimeException("Summary record not found for recalculation"));

        attendance.setEffectiveWorkingHours(totalSeconds / 3600.0);
        attendanceRepository.save(attendance);

        return totalSeconds;
    }

    // specifically updates LastOutTime (useful when logic is separate)
    private void updateLastOutTime(Integer employeeId, LocalDate date, LocalDateTime outTime) {
        Attendance attendance = attendanceRepository.findDailyAttendance(employeeId, date).orElse(null);
        if (attendance != null) {
            attendance.setLastOutTime(outTime);
            attendanceRepository.save(attendance);
        }
    }

    // Utility: Seconds -> HH:MM:SS
    private String formatSecondsToTime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}