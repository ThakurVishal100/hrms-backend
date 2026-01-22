package com.hrms.repository;

import com.hrms.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    // Employee: See my own history
    @Query(value = "SELECT * FROM tb_leave_request WHERE emp_id = :employeeId ORDER BY applied_on DESC", nativeQuery = true)
    List<LeaveRequest> findMyHistory(Long employeeId);

    // HR/Manager: See all  requests by status
    @Query(value = "SELECT * FROM tb_leave_request WHERE status = :status ORDER BY applied_on DESC", nativeQuery = true)
    List<LeaveRequest> findRequestsByStatus(String status);

    // Check for overlapping leave dates (prevent double booking)
    @Query(value = "SELECT * FROM tb_leave_request WHERE emp_id = :empId " +
            "AND status NOT IN ('REJECTED', 'CANCELLED') " +
            "AND start_date <= :end AND end_date >= :start", nativeQuery = true)
    List<LeaveRequest> findOverlappingRequests(Integer empId, java.time.LocalDate start, java.time.LocalDate end);

}