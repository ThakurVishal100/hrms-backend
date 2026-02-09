package com.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_employee_record")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id")
    private Integer employeeId;

    @Column(name = "employee_name")
    private String employeeName;

    @Column(name = "login_id", unique = true)
    private String loginId;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne
    @JoinColumn(name = "dept_id")
    private Department department;

    @ManyToOne
    @JoinColumn(name = "designation_id")
    private Designation designation;

    @Column(name = "reg_date")
    private LocalDateTime regDate;

    @Column(name = "joining_date")
    private LocalDate joiningDate;

    @Column(name = "relieving_date")
    private LocalDate relievingDate;

    // 0=inactive, -1=blocked, 1=active, -99=left, -2=LWP, 2=verified
    private Integer status;

    @Column(name = "status_description")
    private String statusDescription;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "biometric_code", unique = true)
    private Integer biometricCode;
}