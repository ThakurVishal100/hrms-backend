package com.hrms.controller;

import com.hrms.entity.Menu;
import com.hrms.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
@CrossOrigin(origins = "http://localhost:5173")
public class MenuController {

    @Autowired
    private MenuService menuService;


    @GetMapping("/{roleId}")
    public ResponseEntity<List<Menu>> getUserMenus(@PathVariable Long roleId) {
        List<Menu> menus = menuService.getMenusForRole(roleId);

        if (menus.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(menus);
    }
}

