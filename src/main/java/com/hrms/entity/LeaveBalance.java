package com.hrms.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_leave_balance", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"emp_id", "leave_type_id"}) // One balance record per type per employee
})
@Data
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "balance_id")
    private Integer balanceId;

    @ManyToOne
    @JoinColumn(name = "emp_id", nullable = false)
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "current_balance", nullable = false)
    private Double currentBalance; // Double to allow half-days

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.lastUpdated = LocalDateTime.now();
    }
}