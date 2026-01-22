package com.hrms.repository;

import com.hrms.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {

    @Query(value = "SELECT m.* FROM tb_menu_details m " +
            "JOIN tb_menu_access ma ON m.menu_id = ma.menu_id " +
            "WHERE ma.role_id = :roleId " +
            "AND m.status = 1 AND ma.status = 1 " +
            "ORDER BY m.show_order ASC", nativeQuery = true)
    List<Menu> findMenusByRoleId(Long roleId);
}

