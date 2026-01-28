package com.hrms.service;

import com.hrms.entity.User; // Must use User entity
import com.hrms.repository.UserRepository; // Must use User Repo
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        // 1. Look for the user in the User table (NOT Employee table)
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with loginId: " + loginId));

        // 2. Return Spring Security User
        return new org.springframework.security.core.userdetails.User(
                user.getLoginId(),
                user.getPassword(),
                new ArrayList<>()
        );
    }
}