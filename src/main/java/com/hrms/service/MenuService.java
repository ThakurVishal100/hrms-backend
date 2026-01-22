package com.hrms.service;

import com.hrms.entity.Menu;
import com.hrms.repository.MenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MenuService {

    @Autowired
    private MenuRepository menuRepository;

    public List<Menu> getMenusForRole(Long roleId) {
        return menuRepository.findMenusByRoleId(roleId);
    }
}