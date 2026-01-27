package com.hrms.util;

import com.hrms.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    // Ideally, store this in application.properties. Must be at least 256 bits (32 chars)
    public static final String SECRET = "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437";

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();

        // Add Custom Claims (The info frontend needs)
        claims.put("userId", user.getUserId());
        claims.put("loginId", user.getLoginId());
        claims.put("roleId", user.getRole().getRoleId());
        claims.put("roleName", user.getRole().getRoleName());

        if (user.getCompany() != null) {
            claims.put("companyId", user.getCompany().getCompanyId());
        }
        if (user.getEmployee() != null) {
            claims.put("employeeId", user.getEmployee().getEmployeeId());
            claims.put("fullName", user.getEmployee().getEmployeeName());
        }

        return createToken(claims, user.getLoginId());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 Hours Expiry
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}