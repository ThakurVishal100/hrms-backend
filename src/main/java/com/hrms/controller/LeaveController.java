package com.hrms.controller;

import com.hrms.dto.LeaveBalanceDTO;
import com.hrms.dto.LeaveRequestDTO;
import com.hrms.entity.LeaveRequest;
import com.hrms.service.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leaves")
@CrossOrigin(origins = "http://localhost:5173")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    // 1. Get My Balances
    @GetMapping("/balance/{empId}")
    public ResponseEntity<List<LeaveBalanceDTO>> getBalances(@PathVariable Long empId) {
        return ResponseEntity.ok(leaveService.getEmployeeBalances(empId));
    }

    // 2. Apply for Leave
    @PostMapping("/apply")
    public ResponseEntity<?> applyForLeave(@RequestBody LeaveRequestDTO dto) {
        try {
            String result = leaveService.applyForLeave(dto);
            return ResponseEntity.ok(Map.of("message", result, "status", "success"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage(), "status", "error"));
        }
    }

    // 3. Get My History
    @GetMapping("/history/{empId}")
    public ResponseEntity<List<LeaveRequest>> getHistory(@PathVariable Long empId) {
        return ResponseEntity.ok(leaveService.getMyHistory(empId));
    }

    // 4. Get Pending Requests (For HR Dashboard)
    @GetMapping("/pending")
    public ResponseEntity<List<LeaveRequest>> getPendingRequests() {
        return ResponseEntity.ok(leaveService.getAllPendingRequests());
    }

    // 5. Approve/Reject Request
    @PostMapping("/process/{requestId}")
    public ResponseEntity<?> processLeave(@PathVariable Long requestId,
                                          @RequestBody Map<String, String> payload) {
        try {
            String status = payload.get("status");
            String comment = payload.get("comment");
            String result = leaveService.processLeave(requestId, status, comment);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}