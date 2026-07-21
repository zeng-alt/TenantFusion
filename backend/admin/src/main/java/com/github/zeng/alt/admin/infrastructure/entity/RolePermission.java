package com.github.zeng.alt.admin.infrastructure.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.zeng.alt.domain.base.BaseEntity;
import com.github.zeng.alt.domain.key.SnowflakeId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SoftDelete;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "main_role_permission")
@Getter @Setter
@SQLDelete(sql = """
    update main_role_permission
    set is_deleted=true
    where role_permission_id=?
""")
@SQLRestriction("is_deleted=false")
public class RolePermission extends BaseEntity<Long> {

    @Id @SnowflakeId
    private Long rolePermissionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id")
    private Permission permission;

    @Column(name = "is_deleted")
    private Boolean deleted = false;

    @Override
    @JsonIgnore
    public @Nullable Long getId() {
        return rolePermissionId;
    }
}