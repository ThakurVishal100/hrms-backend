package com.hrms.repository;

import com.hrms.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findByCompany_CompanyId(Long companyId);

    Optional<Department> findByDeptNameAndCompany_CompanyId(String deptName, Integer companyId);}