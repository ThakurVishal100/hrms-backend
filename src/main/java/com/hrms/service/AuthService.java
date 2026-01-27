package com.hrms.service;

import com.hrms.dto.AuthResponse;
import com.hrms.dto.LoginRequest;
import com.hrms.entity.User;
import com.hrms.repository.UserRepository;
import com.hrms.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // NOTE: 'registerUser' method is removed because we are using
    // Manual SQL for Super Admin and Auto-Creation for Employees.

    public AuthResponse loginUser(LoginRequest request) {
        // 1. Find User by Login ID
        Optional<User> userOpt = userRepository.findByLoginId(request.getLoginId());

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // 2. Check Password
            if (user.getPassword().equals(request.getPassword())) {

                // 3. Update Last Login Time
                user.setLastLogin(LocalDateTime.now());
                String token = jwtUtil.generateToken(user);
                user.setAuthToken(token);
                userRepository.save(user);

//                // 4. Handle Company ID (Safe Check)
//                Integer companyId = null;
//                if (user.getCompany() != null) {
//                    companyId = user.getCompany().getCompanyId();
//                }
//
//                Integer empId = null;
//                if (user.getEmployee() != null) {
//                    empId = user.getEmployee().getEmployeeId();
//                }

                // 5. Build Response
                return AuthResponse.builder()
                        .status("success")
                        .message("Login Successful")
                        .token(token)
//                        .userId(user.getUserId())
//                        .loginId(user.getLoginId())
//                        .roleId(user.getRole().getRoleId())
//                        .roleName(user.getRole().getRoleName())
//                        .companyId(companyId)
//                        .employeeId(empId)
                        .build();
            }
        }

        return AuthResponse.builder()
                .status("error")
                .message("Invalid Credentials or User Not Found")
                .build();
    }
}