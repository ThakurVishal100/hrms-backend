package com.hrms.controller;

import com.hrms.dto.AuthResponse;
import com.hrms.dto.LoginRequest;
import com.hrms.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    @Autowired
    private AuthService authService;


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.loginUser(request);

        if ("error".equals(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        return ResponseEntity.ok(response);
    }
}