package com.hrms.controller;

import com.hrms.entity.Department;
import com.hrms.entity.Designation;
import com.hrms.repository.DepartmentRepository;
import com.hrms.repository.DesignationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/master")
@CrossOrigin(origins = "http://localhost:5173")
public class DetailsController {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DesignationRepository designationRepository;

    @GetMapping("/departments")
    public List<Department> getDepartments(@RequestParam Long companyId) {
        return departmentRepository.findByCompany_CompanyId(companyId);
    }

    @GetMapping("/designations")
    public List<Designation> getDesignations(@RequestParam Long companyId) {
        return designationRepository.findByCompany_CompanyId(companyId);
    }
}