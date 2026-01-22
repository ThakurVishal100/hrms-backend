package com.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_employee_docs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDoc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doc_id")
    private Long docId;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne
    @JoinColumn(name = "dept_id")
    private Department department;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "doc_type")
    private String docType;

    @Column(name = "doc_name")
    private String docName;

    @Column(name = "image_path")
    private String imagePath;

    @Column(name = "image_url")
    private String imageUrl;
}