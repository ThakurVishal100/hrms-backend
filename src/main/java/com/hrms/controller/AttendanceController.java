package com.hrms.controller;

import com.hrms.dto.EntryExitCdrDTO; // Import DTO
import com.hrms.repository.AttendanceRepository;
import com.hrms.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "http://localhost:5173")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @PostMapping("/check-in")
    public ResponseEntity<String> checkIn(@RequestParam Integer empId,
                                          @RequestParam(required = false) String location) {
        String loc = (location != null && !location.isEmpty()) ? location : "Unknown Location";
        try {
            return ResponseEntity.ok(attendanceService.punchIn(empId, loc));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/check-out")
    public ResponseEntity<String> checkOut(@RequestParam Integer empId,
                                           @RequestParam(required = false) String location) {
        String loc = (location != null && !location.isEmpty()) ? location : "Unknown Location";
        try {
            return ResponseEntity.ok(attendanceService.punchOut(empId, loc));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/status/{empId}")
    public ResponseEntity<String> getStatus(@PathVariable Integer empId) {
        return ResponseEntity.ok(attendanceService.getCurrentStatus(empId));
    }

    @GetMapping("/logs/{empId}")
    public ResponseEntity<List<EntryExitCdrDTO>> getLogs(@PathVariable Integer empId) {
        return ResponseEntity.ok(attendanceService.getLast30DaysLogs(empId));
    }

    @GetMapping("/report/monthly")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyReport(
            @RequestParam Integer empId,
            @RequestParam int year) {
        return ResponseEntity.ok(attendanceRepository.findMonthlySummary(empId, year));
    }

    @GetMapping("/report/daily")
    public ResponseEntity<List<EntryExitCdrDTO>> getDailyReport(
            @RequestParam Integer empId,
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(attendanceService.getRawLogsForMonth(empId, month, year));
    }
}