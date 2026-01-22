package com.hrms.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class EmployeeDTO {
    private Integer employeeId;
    private String employeeName;
    private LocalDate joiningDate;
    private Integer status;

    private Long departmentId;
    private String departmentName;
    private Long designationId;
    private String designationName;
    private Integer companyId;


}