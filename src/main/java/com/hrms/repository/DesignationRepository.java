package com.hrms.repository;

import com.hrms.entity.Designation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DesignationRepository extends JpaRepository<Designation, Long> {
    // Fetch designations for a specific company
    List<Designation> findByCompany_CompanyId(Long companyId);

    // Optional: Fetch designations by Department if you want dependent dropdowns later
    List<Designation> findByDepartment_DeptId(Long deptId);

    Optional<Designation> findByDesignationNameAndCompany_CompanyId(String designationName, Integer companyId);
}