package com.hrms.controller;

import com.hrms.dto.EmployeeDTO;
import com.hrms.entity.Employee;
import com.hrms.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = "http://localhost:5173")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @GetMapping
    public List<EmployeeDTO> listEmployees(
            @RequestParam Long roleId,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long filterCompanyId) {
        return employeeService.getEmployees(roleId, companyId,filterCompanyId);
    }

    @PostMapping
    public ResponseEntity<?> addEmployee(@RequestBody Map<String, Object> payload) {
        try {
            Employee savedEmp = employeeService.saveEmployeeWithSalary(payload);
            return ResponseEntity.ok(savedEmp);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getEmployee(@PathVariable Integer id) {
        Map<String, Object> data = employeeService.getEmployeeWithSalary(id);
        if (data != null) {
            return ResponseEntity.ok(data);
        }
        return ResponseEntity.notFound().build();
    }

//    @GetMapping("/{id}")
//    public ResponseEntity<Employee> getEmployee(@PathVariable Long id) {
//        Employee employee = employeeService.getEmployeeById(id);
//        if (employee != null) {
//            return ResponseEntity.ok(employee);
//        }
//        return ResponseEntity.notFound().build();
//    }


    @PostMapping("/update/{id}")
    public ResponseEntity<?> updateEmployee(@PathVariable Integer id, @RequestBody Map<String, Object> payload) {
        try {
            employeeService.updateEmployeeWithSalary(id, payload);
            return ResponseEntity.ok("Updated Successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


//    @PostMapping("/update/{id}")
//    public ResponseEntity<?> updateEmployee(@PathVariable Long id, @RequestBody Employee empDetails) {
//        Employee emp = employeeService.getEmployeeById(id);
//
//        if (emp != null) {
//            try {
//                emp.setEmployeeName(empDetails.getEmployeeName());
//                emp.setStatus(empDetails.getStatus());
//                emp.setStatusDescription(empDetails.getStatusDescription());
//                emp.setJoiningDate(empDetails.getJoiningDate());
//                emp.setRelievingDate(empDetails.getRelievingDate());
//
//                // Update Relationships
//                emp.setDepartment(empDetails.getDepartment());
//                emp.setDesignation(empDetails.getDesignation());
//                emp.setCompany(empDetails.getCompany());
//
//                Employee updatedEmployee = employeeService.saveEmployee(emp);
//                return ResponseEntity.ok(updatedEmployee);
//            } catch (RuntimeException e) {
//                return ResponseEntity.badRequest().body(e.getMessage());
//            }
//        }
//        return ResponseEntity.notFound().build();
//    }

    @PostMapping("/delete/{id}")
    public ResponseEntity<String> deleteEmployee(@PathVariable Integer id) {
        Employee emp = employeeService.getEmployeeById(id);

        if (emp != null) {
            emp.setStatus(0);
            emp.setStatusDescription("Deactivated");
            employeeService.saveEmployee(emp,null);
            return ResponseEntity.ok("Employee deactivated successfully");
        }
        return ResponseEntity.notFound().build();
    }
}

