package com.hrms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrms.dto.EmployeeDTO;
import com.hrms.entity.*;
import com.hrms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeSalaryRepository employeeSalaryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private DesignationRepository designationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public List<EmployeeDTO> getEmployees(Long userRoleId, Long userCompanyId, Long filterCompanyId) {
        List<Employee> employees;

        // 1. Super Admin (Role 1)
        if (userRoleId == 1) {
            // If Admin selected a specific company from dropdown, filter by it
            if (filterCompanyId != null && filterCompanyId > 0) {
                employees = employeeRepository.findByCompanyId(filterCompanyId);
            } else {
                // Otherwise show ALL employees (Default view for Super Admin)
                employees = employeeRepository.findAll();
            }
        }
        // 2. HR / Company Admin (Other Roles)
        else {
            employees = employeeRepository.findByCompanyId(userCompanyId);
        }

        return employees.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }



//    @Transactional
//    public Employee saveEmployeeWithSalary(Map<String, Object> payload) {
//
//        Employee emp = new Employee();
//        emp.setEmployeeName((String) payload.get("employeeName"));
//
//
//        if (payload.get("joiningDate") != null) {
//            emp.setJoiningDate(LocalDate.parse((String) payload.get("joiningDate")));
//        }
//
//
//        if (payload.get("status") != null) {
//            emp.setStatus(Integer.parseInt(String.valueOf(payload.get("status"))));
//        }
//
//
//        Map<?,?> compMap = (Map<?,?>) payload.get("company");
//        if(compMap != null) {
//            Company c = new Company();
//            c.setCompanyId(Integer.valueOf(String.valueOf(compMap.get("companyId"))));
//            emp.setCompany(c);
//        }
//
//        Map<?,?> deptMap = (Map<?,?>) payload.get("department");
//        if(deptMap != null) {
//            Department d = new Department();
//            d.setDeptId(Long.valueOf(String.valueOf(deptMap.get("deptId"))));
//            emp.setDepartment(d);
//        }
//
//        Map<?,?> desgMap = (Map<?,?>) payload.get("designation");
//        if(desgMap != null) {
//            Designation d = new Designation();
//            d.setDesignationId(Long.valueOf(String.valueOf(desgMap.get("designationId"))));
//            emp.setDesignation(d);
//        }
//
//
//        Employee savedEmp = saveEmployee(emp);
//
//
//        if (payload.containsKey("grossSalary") || payload.containsKey("salaryBreakup")) {
//            EmployeeSalary salary = new EmployeeSalary();
//            salary.setEmployee(savedEmp);
//            salary.setCompany(savedEmp.getCompany());
//            salary.setDepartment(savedEmp.getDepartment());
//            salary.setDesignation(savedEmp.getDesignation());
//            salary.setStatus(1);
//            salary.setRegDate(LocalDateTime.now());
//
//
//            if (payload.get("grossSalary") != null && !payload.get("grossSalary").toString().isEmpty()) {
//                salary.setGrossSalaryFixed(new BigDecimal(String.valueOf(payload.get("grossSalary"))));
//            }
//
//
//            if (payload.get("grossSalaryVariable") != null && !payload.get("grossSalaryVariable").toString().isEmpty()) {
//                salary.setGrossSalaryVariable(new BigDecimal(String.valueOf(payload.get("grossSalaryVariable"))));
//            }
//
//            if (payload.get("salaryBreakup") != null) {
//                Map<String, Object> breakupMap = objectMapper.convertValue(
//                        payload.get("salaryBreakup"),
//                        new TypeReference<Map<String, Object>>() {}
//                );
//                salary.setSalaryBreakup(breakupMap);
//            }
//
//            employeeSalaryRepository.save(salary);
//        }
//
//        return savedEmp;
//    }

    @Transactional
    public Employee saveEmployeeWithSalary(Map<String, Object> payload) {
        String empName = (String) payload.get("employeeName");

        // 1. EXTRACT CREDENTIALS
        String customLoginId = (String) payload.get("loginId");
        String customPassword = (String) payload.get("password");

        // 2. EXTRACT COMPANY ID
        Integer companyId = null;
        Map<?,?> compMap = (Map<?,?>) payload.get("company");
        if(compMap != null) {
            companyId = Integer.valueOf(String.valueOf(compMap.get("companyId")));
        } else {
            throw new RuntimeException("Company Information is missing");
        }

        // --- ROBUST UNIQUENESS CHECKS ---

        // A. Login ID Check (MUST BE UNIQUE)
        if (customLoginId != null && !customLoginId.trim().isEmpty()) {
            if (employeeRepository.existsByLoginId(customLoginId)) {
                throw new RuntimeException("Login ID '" + customLoginId + "' is already taken. Please choose a different one (e.g., " + customLoginId + "1).");
            }
        } else {
            throw new RuntimeException("Login ID is required to create an employee.");
        }

        // B. Biometric Code Check (MUST BE UNIQUE for Attendance)
        if (payload.get("biometricCode") != null) {
            Integer bioCode = Integer.parseInt(String.valueOf(payload.get("biometricCode")));
            // Only check if it's > 0 (some might use 0 as a placeholder)
            if (bioCode > 0 && employeeRepository.existsByBiometricCode(bioCode)) {
                throw new RuntimeException("Biometric Code '" + bioCode + "' is already assigned to another employee.");
            }
        }

        // Note: We intentionally REMOVED the Name check.
        // Two "Rahul Sharma"s can exist, provided they have different Login IDs and Biometric Codes.

        // --- PREPARE EMPLOYEE OBJECT ---
        Employee emp = new Employee();
        emp.setEmployeeName(empName);
        emp.setLoginId(customLoginId); // Set the custom Login ID

        if (payload.get("joiningDate") != null) {
            emp.setJoiningDate(LocalDate.parse((String) payload.get("joiningDate")));
        }
        if (payload.get("status") != null) {
            emp.setStatus(Integer.parseInt(String.valueOf(payload.get("status"))));
        }
        if (payload.get("biometricCode") != null) {
            emp.setBiometricCode(Integer.parseInt(String.valueOf(payload.get("biometricCode"))));
        }

        Company c = new Company();
        c.setCompanyId(companyId);
        emp.setCompany(c);

        Map<?,?> deptMap = (Map<?,?>) payload.get("department");
        if(deptMap != null) {
            Department d = new Department();
            d.setDeptId(Long.valueOf(String.valueOf(deptMap.get("deptId"))));
            emp.setDepartment(d);
        }

        Map<?,?> desgMap = (Map<?,?>) payload.get("designation");
        if(desgMap != null) {
            Designation d = new Designation();
            d.setDesignationId(Long.valueOf(String.valueOf(desgMap.get("designationId"))));
            emp.setDesignation(d);
        }

        // --- SAVE EMPLOYEE & USER (Pass Password) ---
        Employee savedEmp = saveEmployee(emp, customPassword);

        // --- SAVE SALARY (Existing Logic) ---
        if (payload.containsKey("grossSalary") || payload.containsKey("salaryBreakup")) {
            EmployeeSalary salary = new EmployeeSalary();
            salary.setEmployee(savedEmp);
            salary.setCompany(savedEmp.getCompany());
            salary.setDepartment(savedEmp.getDepartment());
            salary.setDesignation(savedEmp.getDesignation());
            salary.setStatus(1);
            salary.setRegDate(LocalDateTime.now());

            if (payload.get("grossSalary") != null && !payload.get("grossSalary").toString().isEmpty()) {
                salary.setGrossSalaryFixed(new BigDecimal(String.valueOf(payload.get("grossSalary"))));
            }
            if (payload.get("grossSalaryVariable") != null && !payload.get("grossSalaryVariable").toString().isEmpty()) {
                salary.setGrossSalaryVariable(new BigDecimal(String.valueOf(payload.get("grossSalaryVariable"))));
            }
            if (payload.get("salaryBreakup") != null) {
                Map<String, Object> breakupMap = objectMapper.convertValue(
                        payload.get("salaryBreakup"),
                        new TypeReference<Map<String, Object>>() {}
                );
                salary.setSalaryBreakup(breakupMap);
            }
            employeeSalaryRepository.save(salary);
        }

        return savedEmp;
    }

    public Map<String, Object> getEmployeeWithSalary(Integer id) {
        Employee emp = employeeRepository.findById(id).orElse(null);
        if (emp == null) return null;

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());


//        Map<String, Object> result = mapper.convertValue(emp, Map.class);
        Map<String, Object> result = mapper.convertValue(emp, new TypeReference<Map<String, Object>>() {});

        Optional<EmployeeSalary> salaryOpt = employeeSalaryRepository.findByEmployee_EmployeeId(id);
        if (salaryOpt.isPresent()) {
            result.put("grossSalary", salaryOpt.get().getGrossSalaryFixed());
            result.put("grossSalaryVariable", salaryOpt.get().getGrossSalaryVariable());
            result.put("salaryBreakup", salaryOpt.get().getSalaryBreakup());
        } else {
            result.put("grossSalary", "");
            result.put("grossSalaryVariable", "");
            result.put("salaryBreakup", null);
        }

        return result;
    }

    @Transactional
    public Employee updateEmployeeWithSalary(Integer id, Map<String, Object> payload) {
        Employee emp = employeeRepository.findById(id).orElseThrow(() -> new RuntimeException("Employee not found"));

        if(payload.get("employeeName") != null) emp.setEmployeeName((String) payload.get("employeeName"));
        if(payload.get("joiningDate") != null) emp.setJoiningDate(LocalDate.parse((String) payload.get("joiningDate")));
        if(payload.get("status") != null) emp.setStatus(Integer.parseInt(String.valueOf(payload.get("status"))));

        // Update Relationships if IDs changed
        Map<?,?> deptMap = (Map<?,?>) payload.get("department");
        if(deptMap != null) {
            Department d = new Department();
            d.setDeptId(Long.valueOf(String.valueOf(deptMap.get("deptId"))));
            emp.setDepartment(d);
        }
        Map<?,?> desgMap = (Map<?,?>) payload.get("designation");
        if(desgMap != null) {
            Designation d = new Designation();
            d.setDesignationId(Long.valueOf(String.valueOf(desgMap.get("designationId"))));
            emp.setDesignation(d);
        }

        Employee savedEmp = employeeRepository.save(emp);


        if (payload.containsKey("grossSalary") || payload.containsKey("salaryBreakup")) {

            EmployeeSalary salary = employeeSalaryRepository.findByEmployee_EmployeeId(id)
                    .orElse(new EmployeeSalary());

            salary.setEmployee(savedEmp);
            salary.setCompany(savedEmp.getCompany());
            salary.setDepartment(savedEmp.getDepartment());
            salary.setDesignation(savedEmp.getDesignation());
            salary.setStatus(1);
            if(salary.getRegDate() == null) salary.setRegDate(LocalDateTime.now());


            if (payload.containsKey("grossSalary") && payload.get("grossSalary") != null) {
                salary.setGrossSalaryFixed(new BigDecimal(String.valueOf(payload.get("grossSalary"))));
            }


            if (payload.containsKey("grossSalaryVariable") && payload.get("grossSalaryVariable") != null) {
                salary.setGrossSalaryVariable(new BigDecimal(String.valueOf(payload.get("grossSalaryVariable"))));
            }

            if (payload.get("salaryBreakup") != null) {
                if (payload.get("salaryBreakup") != null) {
                    Map<String, Object> breakupMap = objectMapper.convertValue(
                            payload.get("salaryBreakup"),
                            new TypeReference<Map<String, Object>>() {}
                    );
                    salary.setSalaryBreakup(breakupMap);
                }
            }

            employeeSalaryRepository.save(salary);
        }

        return savedEmp;
    }


//    @Transactional
//    public Employee saveEmployee(Employee employee) {
//        return saveEmployeeDetails(employee, null);
//    }

    @Transactional
    public Employee saveEmployee(Employee employee, String password) {

        //  VALIDATION
        if (employee.getEmployeeName() == null || employee.getEmployeeName().trim().isEmpty()) {
            throw new RuntimeException("Employee Name is required.");
        }
        if (employee.getCompany() == null || employee.getCompany().getCompanyId() == null) {
            throw new RuntimeException("Company ID is required.");
        }
        if (employee.getDepartment() == null || employee.getDepartment().getDeptId() == null) {
            throw new RuntimeException("Department is required.");
        }
        if (employee.getDesignation() == null || employee.getDesignation().getDesignationId() == null) {
            throw new RuntimeException("Designation is required.");
        }
        if (employee.getJoiningDate() == null) {
            throw new RuntimeException("Joining Date is required.");
        }
        if (employee.getLoginId() == null || employee.getLoginId().trim().isEmpty()) {
            throw new RuntimeException("Login ID is required for user creation.");
        }

        // DEFAULTS
        if (employee.getStatusDescription() == null) employee.setStatusDescription("Active");
        if (employee.getRegDate() == null) employee.setRegDate(LocalDateTime.now());
        if (employee.getStatus() == null) employee.setStatus(1);

        Employee savedEmployee = employeeRepository.save(employee);

        // CREATE USER LOGIN
        // We now force update/create if they don't exist, using provided credentials
        createUserForEmployee(savedEmployee, password);

        return savedEmployee;
    }

    private void createUserForEmployee(Employee employee, String password) {
        // Check if user already exists
        Integer userCount = userRepository.countByEmployeeId(employee.getEmployeeId());

        if (userCount != null && userCount > 0) {
            return; // User already exists
        }

        User newUser = new User();
        newUser.setLoginId(employee.getLoginId()); // Use the ID from Employee Entity
        newUser.setPassword(password != null && !password.isEmpty() ? password : "welcome123"); // Use provided password
        newUser.setEmployee(employee);
        newUser.setCompany(employee.getCompany());

        // Assign Role: Default to "Employee" (ID 3)
        // Adjust ID if your DB uses different IDs for Role
        Role employeeRole = roleRepository.findById(3L).orElse(null);
        newUser.setRole(employeeRole);

        newUser.setStatus(1);
        newUser.setRegDate(LocalDateTime.now());

        userRepository.save(newUser);
        System.out.println(">>> User Created: " + employee.getLoginId());
    }

    public Employee getEmployeeById(Integer id) {
        return employeeRepository.findById(id).orElse(null);
    }

    public void deleteEmployee(Integer id) {
        employeeRepository.deleteById(id);
    }


    public EmployeeDTO convertToDTO(Employee emp) {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setEmployeeId(emp.getEmployeeId());
        dto.setEmployeeName(emp.getEmployeeName());
        dto.setJoiningDate(emp.getJoiningDate());
        dto.setStatus(emp.getStatus());
        dto.setCompanyId(emp.getCompany() != null ? emp.getCompany().getCompanyId() : null);

        if (emp.getDepartment() != null) {
            dto.setDepartmentId(emp.getDepartment().getDeptId());
            dto.setDepartmentName(emp.getDepartment().getDeptName());
        }
        if (emp.getDesignation() != null) {
            dto.setDesignationId(emp.getDesignation().getDesignationId());
            dto.setDesignationName(emp.getDesignation().getDesignationName());
        }

        return dto;
    }
}