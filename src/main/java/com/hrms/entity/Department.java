package com.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_dept")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dept_id")
    private Long deptId;

    @Column(name = "dept_name")
    private String deptName;

    private Integer status;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;
}