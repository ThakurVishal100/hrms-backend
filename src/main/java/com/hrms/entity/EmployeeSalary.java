package com.hrms.entity;

import com.hrms.converter.JsonMapConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.Map;

@Entity
@Table(name = "tb_employee_salary")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSalary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "salary_id")
    private Long salaryId;

    @ManyToOne
    @JoinColumn(name = "emp_id")
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne
    @JoinColumn(name = "dept_id")
    private Department department;

    @ManyToOne
    @JoinColumn(name = "designation_id")
    private Designation designation;

    @Column(name = "gross_salary_fixed")
    private BigDecimal grossSalaryFixed;

    @Column(name = "gross_salary_variable")
    private BigDecimal grossSalaryVariable;

    @Column(name = "salary_breakup", columnDefinition = "json")
    @Convert(converter = JsonMapConverter.class)
    private Map<String, Object> salaryBreakup;


    private Integer status;

    @Column(name = "reg_date")
    private LocalDateTime regDate;
}