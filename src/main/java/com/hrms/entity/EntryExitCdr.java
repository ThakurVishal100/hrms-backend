package com.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_employee_entry_exit_cdr")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntryExitCdr {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cdr_id")
    private Long cdrId;

    @Column(name = "cdr_date")
    private LocalDate cdrDate;

    @ManyToOne
    @JoinColumn(name = "emp_id")
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "in_time")
    private LocalDateTime inTime;

    @Column(name = "out_time")
    private LocalDateTime outTime;

    @Column(name = "in_location", length = 500)
    private String inLocation;

    @Column(name = "out_location", length = 500)
    private String outLocation;

    @Column(name = "updatetime_intime_entry")
    private LocalDateTime updateTimeInTimeEntry;

    @Column(name = "updatetime_outtime_entry")
    private LocalDateTime updateTimeOutTimeEntry;
}