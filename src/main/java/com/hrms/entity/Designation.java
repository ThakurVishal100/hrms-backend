package com.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_designation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Designation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "designation_id")
    private Long designationId;

    @Column(name = "designation_name")
    private String designationName;

    @ManyToOne
    @JoinColumn(name = "dept_id")
    private Department department;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;
}