package com.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_menu_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_id")
    private Long menuId;

    @Column(name = "menu_name")
    private String menuName;

    @Column(name = "menu_desc")
    private String menuDesc;

    // Self-referencing relationship for Sub-menus
    @Column(name = "parent_menu_id")
    private Long parentMenuId;

    @Column(name = "show_order")
    private Integer showOrder;

    @Column(name = "target_page")
    private String targetPage;

    @Column(name = "has_submenu")
    private Integer hasSubmenu;

    @Column(name = "is_submenu")
    private Integer isSubmenu;

    @Column(name = "reg_date")
    private LocalDateTime regDate;

    @Column(name = "last_update")
    private LocalDateTime lastUpdate;

    private Integer status;
}