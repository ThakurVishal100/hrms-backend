package com.hrms.dto;

import lombok.Data;

@Data
public class MonthlyAttendanceDTO {
    private Integer month;
    private Long daysPresent;
    private Double totalHours;
}