package com.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_company_reg")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_id")
    private Integer companyId;

    @Column(name = "company_name")
    private String companyName;

    private Integer status;

    @Column(name = "reg_date")
    private LocalDateTime regDate;

    @Column(name = "service_start_date")
    private LocalDate serviceStartDate;

    @Column(name = "service_expire_date")
    private LocalDate serviceExpireDate;
}