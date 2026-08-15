package com.github.zeng.alt.camunda.identity.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 只读映射 admin 的 main_user_role 关联表。
 */
@Entity
@Table(name = "main_user_role")
@Getter
@Setter
public class MainUserRoleEntity {

    @Id
    @Column(name = "user_role_id")
    private Long userRoleId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private MainUserEntity user;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private MainRoleEntity role;
}
