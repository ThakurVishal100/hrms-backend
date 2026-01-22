package com.hrms.service;

import com.hrms.dto.LeaveBalanceDTO;
import com.hrms.dto.LeaveRequestDTO;
import com.hrms.entity.*;
import com.hrms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaveService {

    @Autowired
    private LeaveBalanceRepository balanceRepository;

    @Autowired
    private LeaveRequestRepository requestRepository;

    @Autowired
    private LeaveTypeRepository typeRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    // 1. GET MY LEAVE BALANCES
    public List<LeaveBalanceDTO> getEmployeeBalances(Long empId) {
        List<LeaveBalance> balances = balanceRepository.findByEmployeeId(empId);

        return balances.stream().map(b -> {
            LeaveBalanceDTO dto = new LeaveBalanceDTO();
            dto.setLeaveTypeId(b.getLeaveType().getLeaveTypeId());
            dto.setLeaveType(b.getLeaveType().getTypeName());
            dto.setDescription(b.getLeaveType().getDescription());
            dto.setAnnualQuota(Double.valueOf(b.getLeaveType().getAnnualQuota()));
            dto.setCurrentBalance(b.getCurrentBalance());
            return dto;
        }).collect(Collectors.toList());
    }

    // 2. APPLY FOR LEAVE
    @Transactional
    public String applyForLeave(LeaveRequestDTO dto) {
        // A. Basic Validations
        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new RuntimeException("Start Date cannot be after End Date");
        }

        // B. Calculate Duration (excluding weekends? For now, let's keep it simple: Total Days)
        long daysDiff = ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) + 1;
        Double requestedDays = (double) daysDiff;

        // C. Check if Leave Type exists
        LeaveType type = typeRepository.findById(dto.getLeaveTypeId())
                .orElseThrow(() -> new RuntimeException("Invalid Leave Type"));

        // D. Check Overlap (Don't double book)
        List<LeaveRequest> overlaps = requestRepository.findOverlappingRequests(dto.getEmpId(), dto.getStartDate(), dto.getEndDate());
        if (!overlaps.isEmpty()) {
            throw new RuntimeException("You already have a leave request for these dates!");
        }

        // E. Check Balance (Only if not LWP)
        if (!type.getTypeName().contains("Loss of Pay")) {
            LeaveBalance balance = balanceRepository.findByEmpAndType(dto.getEmpId(), dto.getLeaveTypeId())
                    .orElseThrow(() -> new RuntimeException("Leave Balance record not found"));

            if (balance.getCurrentBalance() < requestedDays) {
                throw new RuntimeException("Insufficient Balance! You have " + balance.getCurrentBalance() + " days left.");
            }
        }

        // F. Save Request (Status = PENDING)
        // Note:  do NOT deduct balance yet.  deduct on Approval.
        Employee emp = employeeRepository.findById(dto.getEmpId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        LeaveRequest request = new LeaveRequest();
        request.setEmployee(emp);
        request.setLeaveType(type);
        request.setStartDate(dto.getStartDate());
        request.setEndDate(dto.getEndDate());
        request.setTotalDays(requestedDays);
        request.setReason(dto.getReason());
        request.setStatus("PENDING");

        requestRepository.save(request);

        return "Leave Application Submitted successfully!";
    }

    // 3. GET MY HISTORY
    public List<LeaveRequest> getMyHistory(Long empId) {
        return requestRepository.findMyHistory(empId);
    }

    // 4. GET PENDING REQUESTS (For Manager)
    public List<LeaveRequest> getAllPendingRequests() {
        return requestRepository.findRequestsByStatus("PENDING");
    }

    // 5. APPROVE / REJECT LEAVE
    @Transactional
    public String processLeave(Long requestId, String status, String comment) {
        LeaveRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!request.getStatus().equals("PENDING")) {
            throw new RuntimeException("Request is already processed!");
        }

        if (status.equalsIgnoreCase("APPROVED")) {
            request.setStatus("APPROVED");

            // DEDUCT BALANCE NOW
            if (!request.getLeaveType().getTypeName().contains("Loss of Pay")) {
                LeaveBalance balance = balanceRepository.findByEmpAndType(
                        request.getEmployee().getEmployeeId(),
                        request.getLeaveType().getLeaveTypeId()
                ).orElseThrow(() -> new RuntimeException("Balance not found"));

                balance.setCurrentBalance(balance.getCurrentBalance() - request.getTotalDays());
                balanceRepository.save(balance);
            }

        } else {
            request.setStatus("REJECTED");
        }

        request.setManagerComments(comment);
        requestRepository.save(request);

        return "Leave request " + status;
    }
}