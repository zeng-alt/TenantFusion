package com.github.zeng.alt.admin.infrastructure.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.zeng.alt.domain.base.BaseEntity;
import com.github.zeng.alt.domain.key.SnowflakeId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "main_user_role")
@Getter @Setter
public class UserRole extends BaseEntity<Long> {

    @Id @SnowflakeId
    private Long userRoleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @Override
    @JsonIgnore
    public @Nullable Long getId() {
        return userRoleId;
    }
}