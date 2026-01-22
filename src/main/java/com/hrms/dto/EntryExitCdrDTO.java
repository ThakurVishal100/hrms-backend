package com.hrms.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class EntryExitCdrDTO {
    private Long cdrId;
    private LocalDate cdrDate;
    private LocalDateTime inTime;
    private LocalDateTime outTime;
    private String inLocation;
    private String outLocation;

    private String duration;
}