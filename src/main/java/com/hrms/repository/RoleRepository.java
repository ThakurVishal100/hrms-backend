package com.hrms.repository;

import com.hrms.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    @Query(value = "SELECT COUNT(*) FROM tb_roles", nativeQuery = true)
    int countAllRoles();
}