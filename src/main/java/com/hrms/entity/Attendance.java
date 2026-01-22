package com.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_attendance_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "att_id")
    private Long attId;

    @ManyToOne
    @JoinColumn(name = "emp_id")
    private Employee employee;

    @Column(name = "attendance_date")
    private LocalDate attendanceDate;

    @Column(name = "effective_wh")
    private Double effectiveWorkingHours;

    @Column(name = "first_intime")
    private LocalDateTime firstInTime;

    @Column(name = "last_outtime")
    private LocalDateTime lastOutTime;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;
}