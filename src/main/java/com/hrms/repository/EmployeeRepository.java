package com.hrms.repository;

import com.hrms.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    @Query(value = "SELECT COUNT(*) FROM tb_employee_record", nativeQuery = true)
    long countTotalEmployees();

    @Query(value = "SELECT COUNT(*) FROM tb_employee_record WHERE status = 1", nativeQuery = true)
    long countActiveEmployees();

    @Query(value="select * from tb_employee_record where company_id =:companyId", nativeQuery = true)
    List<Employee> findByCompanyId(Long companyId);

    List<Employee> findByBiometricCode(Integer biometricCode);

    Optional<Employee> findByLoginId(String loginId);
}

