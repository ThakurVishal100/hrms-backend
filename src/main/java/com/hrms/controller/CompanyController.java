package com.hrms.controller;

import com.hrms.entity.Company;
import com.hrms.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/companies")
@CrossOrigin(origins = "http://localhost:5173")
public class CompanyController {
    @Autowired
    private CompanyRepository companyRepository;

    @GetMapping
    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }
}