package com.hrms.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String status;
    private String message;
    private Integer userId;
    private String loginId;
    private String roleName;
    private Integer roleId;
    private Integer companyId;

    private Integer employeeId;
}