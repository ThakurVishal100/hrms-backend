package com.hrms.repository;

import com.hrms.entity.EmployeeSalary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EmployeeSalaryRepository extends JpaRepository<EmployeeSalary, Integer> {

    @Query(value = "SELECT * FROM tb_employee_salary WHERE emp_id = :employeeId LIMIT 1", nativeQuery = true)
    Optional<EmployeeSalary> findByEmployee_EmployeeId(Integer employeeId);

    @Query(value = "SELECT * FROM tb_employee_salary WHERE emp_id = :empId AND status = :status", nativeQuery = true)
    Optional<EmployeeSalary> findByEmployee_EmployeeIdAndStatus(Integer empId, Integer status);
}