package com.github.zeng.alt.admin.infrastructure.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.zeng.alt.admin.infrastructure.listener.RoleListener;
import com.github.zeng.alt.tenant.row.TenantBaseEntity;
import com.github.zeng.alt.domain.key.SnowflakeId;
import com.github.zeng.alt.domain.validation.UniqueCheck;
import com.github.zeng.alt.rest.annotation.QueryField;
import com.github.zeng.alt.rest.annotation.QueryOrder;
import com.github.zeng.alt.rest.annotation.QueryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "main_role")
@Getter @Setter
@UniqueCheck(field = "code", ignoreCase = true)
@EntityListeners(RoleListener.class)
public class Role extends TenantBaseEntity<Long> {

    @Id @SnowflakeId
    private Long roleId;

    @Column(length = 64)
    @QueryField(type = QueryType.LIKE)
    private String code;
    @QueryField(type = QueryType.LIKE)
    private String name;
    @QueryOrder(autoSort = true)
    private Integer roleSort;

    @Column(name = "is_enabled")
    @QueryField
    private Boolean enabled = true;

//    @Column(name = "is_deleted")
//    private Boolean deleted = false;

    @JsonIgnore
    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<UserRole> userRoles = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<RolePermission> rolePermissions = new HashSet<>();

    @Override
    @JsonIgnore
    public @Nullable Long getId() {
        return roleId;
    }
}