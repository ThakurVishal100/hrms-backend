package com.hrms.dto;

import lombok.Data;

@Data
public class SignupRequest {
    private String loginId;
    private String password;
//    private Integer roleId;
//    private Long companyId;
    private String companyName;
    private String fullName;
}

