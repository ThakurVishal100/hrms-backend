package com.hrms.repository;

import com.hrms.entity.EmployeeBankDetails;
import com.hrms.entity.EmployeeSalary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeBankDetailsRepository extends JpaRepository<EmployeeBankDetails, Long> {
    @Query(value = "SELECT * FROM tb_employee_bank_details WHERE emp_id = :empId", nativeQuery = true)
    Optional<EmployeeBankDetails> findByEmployee_EmployeeId(Integer empId);
}