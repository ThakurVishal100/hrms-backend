package com.hrms.repository;

import com.hrms.entity.EntryExitCdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

@Repository
public interface EntryExitCdrRepository extends JpaRepository<EntryExitCdr, Long> {

    @Query(value = "SELECT * FROM tb_employee_entry_exit_cdr WHERE emp_id = :empId AND cdr_date = :date AND out_time IS NULL ORDER BY in_time DESC LIMIT 1", nativeQuery = true)
    Optional<EntryExitCdr> findOpenSession(Integer empId, LocalDate date);

    @Query(value = "SELECT * FROM tb_employee_entry_exit_cdr WHERE emp_id = :empId AND cdr_date = :date", nativeQuery = true)
    List<EntryExitCdr> findTodaysCdrs(Integer empId, LocalDate date);

    @Query(value = "SELECT * FROM tb_employee_entry_exit_cdr " +
            "WHERE emp_id = :empId AND out_time IS NULL AND cdr_date < :today " +
            "ORDER BY cdr_date DESC LIMIT 1", nativeQuery = true)
    Optional<EntryExitCdr> findStaleSession(Integer empId, LocalDate today);

    @Query(value = "SELECT * FROM tb_employee_entry_exit_cdr WHERE emp_id = :empId ORDER BY cdr_date DESC, in_time DESC", nativeQuery = true)
    List<EntryExitCdr> findByEmployee_EmployeeIdOrderByCdrDateDesc(Integer empId);

    @Query(value = "SELECT * FROM tb_employee_entry_exit_cdr " +
            "WHERE emp_id = :empId " +
            "AND MONTH(cdr_date) = :month " +
            "AND YEAR(cdr_date) = :year " +
            "ORDER BY cdr_date DESC, in_time DESC", nativeQuery = true)
    List<EntryExitCdr> findRawLogsByMonth(Integer empId, int month, int year);
}