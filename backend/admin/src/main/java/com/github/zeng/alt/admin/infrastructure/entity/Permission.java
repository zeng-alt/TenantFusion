package com.github.zeng.alt.admin.infrastructure.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.zeng.alt.domain.base.BaseEntity;
import com.github.zeng.alt.domain.key.SnowflakeId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

@Entity()
@Table(name = "main_permission")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "resource_type", discriminatorType = DiscriminatorType.STRING, length = 31)
@Getter @Setter
public abstract class Permission extends BaseEntity<Long> {

    @Id
    @SnowflakeId
    private Long permissionId;

    @Column(length = 64)
    private String code;
    private String name;

    @Column(name = "resource_type", insertable = false, updatable = false)
    private String resourceType;

    @Column(length = 500)
    private String description;

    @Column(name = "is_enabled")
    private Boolean enabled = true;

    @JsonIgnore
    @OneToMany(mappedBy = "permission", fetch = FetchType.LAZY)
    private Set<RolePermission> rolePermissions = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "permission", fetch = FetchType.LAZY)
    private Set<PolicyRule> policyRules = new HashSet<>();

    @Override
    @JsonIgnore
    public @Nullable Long getId() {
        return permissionId;
    }
}