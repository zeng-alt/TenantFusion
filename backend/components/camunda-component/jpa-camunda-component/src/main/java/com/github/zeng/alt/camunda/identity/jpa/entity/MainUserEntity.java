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
 * 只读映射 admin 的 main_user 表。
 */
@Entity
@Table(name = "main_user")
@Getter
@Setter
public class MainUserEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 64)
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "nick_name")
    private String nickName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "gender")
    private String gender;

    @Column(name = "is_enabled")
    private Boolean enabled;

    @Column(name = "is_deleted")
    private Boolean deleted;

    @OneToMany(mappedBy = "user")
    private List<MainUserRoleEntity> userRoles = new LinkedList<>();
}
