package com.hrms.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Data
public class BiometricDataDTO {
    private Integer biometricCode;
    private String employeeName;
    private String department;   // New Field
    private String designation;  // New Field

    private Map<LocalDate, TimeEntry> dailyRecords = new HashMap<>();

    @Data
    public static class TimeEntry {
        private LocalTime inTime;
        private LocalTime outTime;
    }
}