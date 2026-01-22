package com.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_menu_access")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne
    @JoinColumn(name = "menu_id")
    private Menu menu;

    private Integer status;

    @Column(name = "last_update")
    private LocalDateTime lastUpdate;
}