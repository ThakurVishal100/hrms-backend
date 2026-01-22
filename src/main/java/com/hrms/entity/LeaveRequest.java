package com.hrms.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_leave_request")
@Data
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Integer requestId;

    @ManyToOne
    @JoinColumn(name = "emp_id", nullable = false)
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_days", nullable = false)
    private Double totalDays; //  2.0 or 0.5

    @Column(name = "reason", length = 500)
    private String reason;

    // Status: PENDING, APPROVED, REJECTED, CANCELLED
    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "manager_comments")
    private String managerComments;

    @Column(name = "applied_on")
    private LocalDateTime appliedOn;

    @PrePersist
    public void onCreate() {
        this.appliedOn = LocalDateTime.now();
        if (this.status == null) {
            this.status = "PENDING";
        }
    }
}