package com.hrms.controller;

import com.hrms.dto.BiometricDataDTO;
import com.hrms.dto.EntryExitCdrDTO; // Import DTO
import com.hrms.repository.AttendanceRepository;
import com.hrms.service.AttendanceService;
import com.hrms.service.ExcelParserService;
import com.hrms.service.ReconciliationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "http://localhost:5173")
public class AttendanceController {

    @Autowired
    private ExcelParserService excelParserService;

    @Autowired
    private ReconciliationService reconciliationService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private AttendanceRepository attendanceRepository;


//    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<?> uploadBiometricFile(@RequestParam("file") MultipartFile file) {
//        try {
//            if (file.isEmpty()) {
//                return ResponseEntity.badRequest().body(Map.of("error", "Please upload a valid Excel/CSV file."));
//            }
//
//            // Step 1: Parse the file into DTOs
//            List<BiometricDataDTO> parsedData = excelParserService.parseBiometricFile(file);
//            System.out.println("Parsed " + parsedData.size() + " employees from file.");
//
//            // Step 2: Run the Reconciliation Logic (Priority Comparison)
////            reconciliationService.reconcileAttendance(parsedData);
//            String statusMessage = attendanceService.syncBiometricAttendance(parsedData);
//
//            return ResponseEntity.ok(Map.of(
//                    "message", "Attendance uploaded and reconciled successfully!",
//                    "details",statusMessage,
//                    "employeesProcessed", parsedData.size()
//            ));
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.internalServerError().body(Map.of("error", "Error processing file: " + e.getMessage()));
//        }
//    }


    // 1. UPLOAD & RECONCILE (The Main Endpoint)
    @PostMapping(value = "/reconcile-full", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> reconcileFull(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "whatsappText", required = false) String whatsappText) {
        try {
            // A. Parse CSV
            if (file.isEmpty()) return ResponseEntity.badRequest().body("File is empty");
            List<BiometricDataDTO> csvData = excelParserService.parseBiometricFile(file);
            System.out.println("Parsed CSV: " + csvData.size() + " employees");

            // B. Parse WhatsApp (if provided)
            Map<String, Map<LocalDate, BiometricDataDTO.TimeEntry>> whatsappData = null;
            if (whatsappText != null && !whatsappText.trim().isEmpty()) {
                whatsappData = excelParserService.parseWhatsAppText(whatsappText);
                System.out.println("Parsed WhatsApp Text for reconciliation.");
            }

            // C. Run Logic
            String result = attendanceService.reconcileAttendance(csvData, whatsappData);

            return ResponseEntity.ok(Map.of(
                    "message", "Reconciliation Successful",
                    "details", result
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }



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

    @PostMapping(value = "/reconcile-multi", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> reconcileMultiSource(
            @RequestParam("file") MultipartFile file,
            @RequestParam("whatsappText") String whatsappText) {
        try {
            List<BiometricDataDTO> excelData = excelParserService.parseBiometricFile(file);
            Map<String, Map<LocalDate, BiometricDataDTO.TimeEntry>> whatsappData = excelParserService.parseWhatsAppText(whatsappText);

            reconciliationService.reconcileAllSources(excelData, whatsappData);

            return ResponseEntity.ok(Map.of("message", "Reconciliation completed successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}