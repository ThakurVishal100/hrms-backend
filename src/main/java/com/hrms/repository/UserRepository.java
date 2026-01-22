package com.hrms.repository;

import com.hrms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginId(String loginId);

    // CHANGED: Returns Integer (Count) instead of Boolean
    // Usage: if result > 0 then Exists
    @Query(value = "SELECT COUNT(*) FROM tb_users WHERE employee_id = :employeeId", nativeQuery = true)
    Integer countByEmployeeId(Integer employeeId);

    // CHANGED: Returns Integer (Count) instead of Boolean
    @Query(value = "SELECT COUNT(*) FROM tb_users WHERE login_id = :loginId", nativeQuery = true)
    Integer countByLoginId(String loginId);


}