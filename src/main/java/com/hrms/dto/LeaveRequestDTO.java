package com.hrms.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class LeaveRequestDTO {
    private Integer empId;
    private Integer leaveTypeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
}