package com.github.zeng.alt.admin.infrastructure.entity;

import app.tozzi.annotation.Searchable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.zeng.alt.domain.base.BaseEntity;
import com.github.zeng.alt.domain.key.SnowflakeId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "main_user")
@Getter
@Setter
@SQLRestriction("is_deleted=false")
public class User extends BaseEntity<Long> {

    @Id @SnowflakeId
    private Long userId;

    @Column(length = 64)
    @Searchable
    private String username;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String nickName;
    private String avatar;
    private String email;
    private String phoneNumber;
    private String gender;
    private String status;           // ACTIVE, LOCKED

    @Column(name = "is_enabled")
    private Boolean enabled = true;

    @Column(name = "is_deleted")
    private Boolean deleted = false;

    @JsonIgnore
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<UserRole> userRoles = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<UserResource> userResources = new HashSet<>();

    @Override
    @JsonIgnore
    public @Nullable Long getId() {
        return userId;
    }
}