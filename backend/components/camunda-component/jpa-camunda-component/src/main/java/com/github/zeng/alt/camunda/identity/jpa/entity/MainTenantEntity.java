package com.github.zeng.alt.camunda.identity.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 只读映射 admin 的 main_tenant 表。
 */
@Entity
@Table(name = "main_tenant")
@Getter
@Setter
public class MainTenantEntity {

    @Id
    @Column(name = "tenant_id", length = 64)
    private String tenantId;

    @Column(name = "tenant_name")
    private String tenantName;

    @Column(name = "is_enabled")
    private Boolean enabled;

    @Column(name = "is_deleted")
    private Boolean deleted;
}
