package com.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_employee_bank_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeBankDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bank_id")
    private Long bankId;


    @OneToOne
    @JoinColumn(name = "emp_id", unique = true)
    private Employee employee;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "ifsc_code")
    private String ifscCode;


    @Column(name = "pan_number")
    private String panNumber;

    @Column(name = "uan_number")
    private String uanNumber;

    @Column(name = "pf_account_number")
    private String pfAccountNumber;

    @Column(name = "esi_number")
    private String esiNumber;
}


