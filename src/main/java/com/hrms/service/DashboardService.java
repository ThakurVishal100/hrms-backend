package com.hrms.service;

import com.hrms.repository.EmployeeRepository;
import com.hrms.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DashboardService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private RoleRepository roleRepository;

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();


//        long totalRoles = roleRepository.countAllRoles();
        long totalEmployees = employeeRepository.countTotalEmployees();
        long activeEmployees = employeeRepository.countActiveEmployees();

//        stats.put("totalRoles", totalRoles);
        stats.put("totalEmployees", totalEmployees);
        stats.put("activeEmployees", activeEmployees);

        return stats;
    }
}

