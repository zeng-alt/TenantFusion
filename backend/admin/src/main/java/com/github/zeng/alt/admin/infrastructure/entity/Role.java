package com.github.zeng.alt.admin.infrastructure.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.zeng.alt.domain.base.BaseEntity;
import com.github.zeng.alt.domain.key.SnowflakeId;
import com.github.zeng.alt.rest.annotation.QueryField;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SoftDelete;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "main_role")
@Getter @Setter
@SQLDelete(sql = """
    update main_role
    set is_deleted=true
    where role_id=?
""")
@SQLRestriction("is_deleted=false")
public class Role extends BaseEntity<Long> {

    @Id @SnowflakeId
    private Long roleId;

    @Column(length = 64)
    private String code;
    private String name;
    private Integer roleSort = 0;

    @QueryField
    @Column(name = "is_enabled")
    private Boolean enabled = true;

    @Column(name = "is_deleted")
    private Boolean deleted = false;

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