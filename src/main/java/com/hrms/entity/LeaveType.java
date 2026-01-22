package com.hrms.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "tb_leave_type_master")
@Data
public class LeaveType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leave_type_id")
    private Integer leaveTypeId;

    @Column(name = "type_name", nullable = false, unique = true)
    private String typeName; //  "Casual Leave", "Sick Leave"

    @Column(name = "annual_quota")
    private Integer annualQuota;

    @Column(name = "is_carry_forward")
    private Integer isCarryForward;

    @Column(name = "description", length = 500)
    private String description;
}