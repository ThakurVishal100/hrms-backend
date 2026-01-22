package com.hrms.dto;

import lombok.Data;

@Data
public class LeaveBalanceDTO {
    private Integer leaveTypeId;
    private String leaveType;
    private String description;
    private Double annualQuota;
    private Double currentBalance;
}