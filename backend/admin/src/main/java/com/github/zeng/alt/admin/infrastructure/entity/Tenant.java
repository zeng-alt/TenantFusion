package com.github.zeng.alt.admin.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 租户实体，映射 main_tenant 表。
 */
@Entity
@Table(name = "main_tenant")
@Getter
@Setter
public class Tenant {

    @Id
    @Column(name = "tenant_id", length = 64)
    private String tenantId;

    @Column(name = "tenant_name")
    private String tenantName;

    @Column(name = "is_enabled")
    private Boolean enabled = true;

    @Column(name = "is_deleted")
    private Boolean deleted = false;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "last_modified_by")
    private String lastModifiedBy;

    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;
}
