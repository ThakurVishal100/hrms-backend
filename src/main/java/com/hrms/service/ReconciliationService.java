package com.hrms.service;

import com.hrms.dto.BiometricDataDTO;
import com.hrms.entity.*;
import com.hrms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
public class ReconciliationService {

    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private DesignationRepository designationRepository;

    @Transactional
    public void reconcileAttendance(List<BiometricDataDTO> excelData) {
        // 1. Ensure Default Company
        Company defaultCompany = companyRepository.findAll().stream().findFirst().orElseGet(() -> {
            Company c = new Company();
            c.setCompanyName("Jiffy Software");
            return companyRepository.save(c);
        });

        // 2. Ensure Default Role (Employee)
        Role empRole = roleRepository.findById(3L).orElseGet(() -> roleRepository.findAll().get(0));

        for (BiometricDataDTO bioEntry : excelData) {
            Integer bioCode = bioEntry.getBiometricCode();
            if (bioCode == null) continue;

            // 3. Find or Create Employee
            List<Employee> existingEmployees = employeeRepository.findByBiometricCode(bioCode);
            Employee employee;

            if (existingEmployees.isEmpty()) {
                // REGISTER NEW EMPLOYEE FULLY
                employee = registerNewEmployeeFull(bioEntry, defaultCompany, empRole);
            } else {
                employee = existingEmployees.get(0);
            }

            // 4. Process Attendance
            if (employee != null) {
                processDailyRecords(employee, bioEntry.getDailyRecords());
            }
        }
    }

    private Employee registerNewEmployeeFull(BiometricDataDTO dto, Company company, Role role) {
        try {
            // A. Find or Create Department
            String deptName = (dto.getDepartment() != null && !dto.getDepartment().isEmpty()) ? dto.getDepartment() : "General";
            Department dept = departmentRepository.findByDeptNameAndCompany_CompanyId(deptName, company.getCompanyId())
                    .orElseGet(() -> {
                        Department d = new Department();
                        d.setDeptName(deptName);
                        d.setCompany(company);
                        return departmentRepository.save(d);
                    });

            // B. Find or Create Designation
            String desigName = (dto.getDesignation() != null && !dto.getDesignation().isEmpty()) ? dto.getDesignation() : "Employee";
            Designation desig = designationRepository.findByDesignationNameAndCompany_CompanyId(desigName, company.getCompanyId())
                    .orElseGet(() -> {
                        Designation d = new Designation();
                        d.setDesignationName(desigName);
                        d.setCompany(company);
                        return designationRepository.save(d);
                    });

            // C. Create Employee
            Employee newEmp = new Employee();
            newEmp.setEmployeeName(dto.getEmployeeName() != null ? dto.getEmployeeName() : "Unknown");
            newEmp.setBiometricCode(dto.getBiometricCode());
            newEmp.setCompany(company);
            newEmp.setDepartment(dept);      // Link Dept
            newEmp.setDesignation(desig);    // Link Desig
            newEmp.setStatus(1);
            newEmp.setJoiningDate(LocalDate.now());
            newEmp = employeeRepository.save(newEmp);

            // D. Create User
            User newUser = new User();
            newUser.setLoginId("EMP" + dto.getBiometricCode());
            newUser.setPassword("password");
            newUser.setEmployee(newEmp);
            newUser.setCompany(company);
            newUser.setRole(role);
            newUser.setStatus(1);
            userRepository.save(newUser);

            System.out.println(">> Created Full User: " + newEmp.getEmployeeName() + " (" + dept.getDeptName() + ")");
            return newEmp;

        } catch (Exception e) {
            System.err.println("Error registering employee " + dto.getEmployeeName() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void processDailyRecords(Employee employee, Map<LocalDate, BiometricDataDTO.TimeEntry> dailyRecords) {
        for (Map.Entry<LocalDate, BiometricDataDTO.TimeEntry> dayEntry : dailyRecords.entrySet()) {
            LocalDate date = dayEntry.getKey();
            BiometricDataDTO.TimeEntry timeRecord = dayEntry.getValue();

            Attendance attendance = attendanceRepository.findByEmployee_EmployeeIdAndAttendanceDate(
                    employee.getEmployeeId(), date
            ).orElse(new Attendance());

            if (attendance.getAttId() == null) {
                attendance.setEmployee(employee);
                attendance.setAttendanceDate(date);
                attendance.setCompany(employee.getCompany());
            }
            attendance.setBioInTime(timeRecord.getInTime());
            attendance.setBioOutTime(timeRecord.getOutTime());
            calculateFinalAttendance(attendance);
            attendanceRepository.save(attendance);
        }
    }

    private void calculateFinalAttendance(Attendance att) {
        LocalTime finalIn = att.getBioInTime() != null ? att.getBioInTime() :
                (att.getWebInTime() != null ? att.getWebInTime() : att.getWhatsappInTime());

        LocalTime finalOut = att.getBioOutTime() != null ? att.getBioOutTime() :
                (att.getWebOutTime() != null ? att.getWebOutTime() : att.getWhatsappOutTime());

        att.setFirstInTime(finalIn != null ? LocalDateTime.of(att.getAttendanceDate(), finalIn) : null);
        att.setLastOutTime(finalOut != null ? LocalDateTime.of(att.getAttendanceDate(), finalOut) : null);

        if (finalIn != null && finalOut != null) {
            long minutes = Duration.between(finalIn, finalOut).toMinutes();
            if (minutes < 0) minutes = 0;
            att.setEffectiveWorkingHours(Math.round((minutes / 60.0) * 100.0) / 100.0);
            att.setStatus(minutes >= 240 ? "Present" : "Half-Day");
        } else {
            att.setEffectiveWorkingHours(0.0);
            att.setStatus((finalIn == null && finalOut == null) ? "Absent" : "Missed Punch");
        }
    }

    @Transactional
    public void reconcileAllSources(List<BiometricDataDTO> excelData, Map<String, Map<LocalDate, BiometricDataDTO.TimeEntry>> whatsappData) {
        // 1. Process Excel Data (By Biometric Code)
        for (BiometricDataDTO bioEntry : excelData) {
            employeeRepository.findByBiometricCode(bioEntry.getBiometricCode()).stream().findFirst().ifPresent(emp -> {
                processDailyRecordsWithSources(emp, bioEntry.getDailyRecords(), "EXCEL");
            });
        }

        // 2. Process WhatsApp Data (By Name Matching)
        for (Map.Entry<String, Map<LocalDate, BiometricDataDTO.TimeEntry>> entry : whatsappData.entrySet()) {
            String empName = entry.getKey();
            employeeRepository.findAll().stream()
                    .filter(e -> e.getEmployeeName().toLowerCase().contains(empName))
                    .findFirst()
                    .ifPresent(emp -> {
                        processDailyRecordsWithSources(emp, entry.getValue(), "WHATSAPP");
                    });
        }
    }

    private void processDailyRecordsWithSources(Employee employee, Map<LocalDate, BiometricDataDTO.TimeEntry> records, String source) {
        for (Map.Entry<LocalDate, BiometricDataDTO.TimeEntry> dayEntry : records.entrySet()) {
            Attendance att = attendanceRepository.findDailyAttendance(employee.getEmployeeId(), dayEntry.getKey())
                    .orElseGet(() -> {
                        Attendance newAtt = new Attendance();
                        newAtt.setEmployee(employee);
                        newAtt.setAttendanceDate(dayEntry.getKey());
                        newAtt.setCompany(employee.getCompany());
                        return newAtt;
                    });

            if ("EXCEL".equals(source)) {
                att.setBioInTime(dayEntry.getValue().getInTime());
                att.setBioOutTime(dayEntry.getValue().getOutTime());
            } else if ("WHATSAPP".equals(source)) {
                att.setWhatsappInTime(dayEntry.getValue().getInTime());
                att.setWhatsappOutTime(dayEntry.getValue().getOutTime());
            }

            // Final recalculation logic using the priority Bio > Web > WhatsApp
            calculateFinalAttendance(att);
            attendanceRepository.save(att);
        }
    }
}