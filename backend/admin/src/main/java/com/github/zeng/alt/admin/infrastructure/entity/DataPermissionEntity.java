package com.github.zeng.alt.admin.infrastructure.entity;

import com.github.zeng.alt.domain.key.SnowflakeId;
import com.github.zeng.alt.security.data.DataPermissionScope;
import com.github.zeng.alt.tenant.row.TenantBaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * 角色在特定查询权限下的数据范围配置。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Entity
@Table(name = "main_data_permission")
@Getter
@Setter
public class DataPermissionEntity extends TenantBaseEntity<Long> {
    @Id
    @SnowflakeId
    private Long dataPermissionId;
    private String resource;
    private String permission;
    private String roleCode;
    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type")
    private DataPermissionScope scope;
    @Column(name = "is_enabled")
    private boolean enabled = true;
    @ElementCollection
    @CollectionTable(name = "main_data_permission_department", joinColumns = @JoinColumn(name = "data_permission_id"))
    @Column(name = "dept_id")
    private Set<Long> departmentIds = new HashSet<>();

    @Override
    public Long getId() {
        return dataPermissionId;
    }
}
