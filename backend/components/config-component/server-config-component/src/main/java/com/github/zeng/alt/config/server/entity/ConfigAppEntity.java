package com.github.zeng.alt.config.server.entity;

import com.github.zeng.alt.domain.base.BaseEntity;
import jakarta.persistence.*;

import java.io.Serial;

@Entity
@Table(name = "sys_config_app")
public class ConfigAppEntity extends BaseEntity<Long> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "app_id")
    private Long appId;

    @Column(name = "app_code", nullable = false, unique = true, length = 64)
    private String appCode;

    @Column(name = "app_name", length = 128)
    private String appName;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "status")
    private Integer status = 1;

    @Column(name = "tenant_id")
    private String tenantId;

    public ConfigAppEntity() {
    }

    @Override
    public Long getId() {
        return appId;
    }

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
