package com.hrms.repository;

import com.hrms.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    //  find all balance
    @Query(value = "SELECT * FROM tb_leave_balance WHERE emp_id = :employeeId", nativeQuery = true)
    List<LeaveBalance> findByEmployeeId(Long employeeId);

    //  find specific balance
    @Query(value="select * from tb_leave_balance where emp_id=:employeeId and leave_type_id = :leaveTypeId", nativeQuery = true)
    Optional<LeaveBalance> findByEmpAndType(Integer employeeId, Integer leaveTypeId);
}