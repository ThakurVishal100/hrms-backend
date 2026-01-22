package com.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_employee_leave_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeLeave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leave_id")
    private Long leaveId;

    @ManyToOne
    @JoinColumn(name = "emp_id")
    private Employee employee;

    @Column(name = "leave_start_date")
    private LocalDate leaveStartDate;

    @Column(name = "leave_end_date")
    private LocalDate leaveEndDate;

    @Column(name = "leave_count")
    private Double leaveCount;

    private Integer status; // 0=applied, 1=approved

    @Column(name = "joining_date_time")
    private LocalDateTime joiningDateTime;
}