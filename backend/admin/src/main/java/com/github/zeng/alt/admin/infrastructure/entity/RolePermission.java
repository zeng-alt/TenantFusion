package com.github.zeng.alt.admin.infrastructure.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.zeng.alt.domain.base.BaseEntity;
import com.github.zeng.alt.domain.key.SnowflakeId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "main_role_permission")
@Getter @Setter
public class RolePermission extends BaseEntity<Long> {

    @Id @SnowflakeId
    private Long rolePermissionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id")
    private Permission permission;

    @Override
    @JsonIgnore
    public @Nullable Long getId() {
        return rolePermissionId;
    }
}