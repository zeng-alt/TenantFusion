package com.github.zeng.alt.camunda.identity.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

/**
 * 只读映射 admin 的 main_role 表。
 */
@Entity
@Table(name = "main_role")
@Getter
@Setter
public class MainRoleEntity {

    @Id
    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "code", length = 64)
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "role_sort")
    private Integer roleSort;

    @Column(name = "is_enabled")
    private Boolean enabled;

    @OneToMany(mappedBy = "role")
    private List<MainUserRoleEntity> userRoles = new LinkedList<>();
}
