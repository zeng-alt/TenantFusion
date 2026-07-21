package com.github.zeng.alt.admin.infrastructure.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.zeng.alt.domain.base.BaseEntity;
import com.github.zeng.alt.domain.key.SnowflakeId;
import com.github.zeng.alt.rest.annotation.QueryField;
import com.github.zeng.alt.rest.annotation.QueryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

@Entity()
@Table(name = "main_permission")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "resource_type", discriminatorType = DiscriminatorType.STRING, length = 31)
@Getter @Setter
@SQLDelete(sql = """
    update main_permission
    set is_deleted=true
    where permission_id=?
""")
@SQLRestriction("is_deleted=false")
public abstract class Permission extends BaseEntity<Long> {

    @Id
    @SnowflakeId
    private Long permissionId;

    @Column(length = 64)
    @QueryField
    private String code;
    @QueryField(type = QueryType.LIKE)
    private String name;

    @Column(name = "resource_type", insertable = false, updatable = false)
    private String resourceType;

    @Column(length = 500)
    private String description;

    @Column(name = "is_enabled")
    private Boolean enabled = true;

    @Column(name = "is_deleted")
    private Boolean deleted = false;

    @JsonIgnore
    @OneToMany(mappedBy = "permission", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
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