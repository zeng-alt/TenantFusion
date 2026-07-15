package com.github.zeng.alt.config.server.entity;

import com.github.zeng.alt.domain.base.BaseEntity;
import jakarta.persistence.*;

import java.io.Serial;

@Entity
@Table(name = "sys_config_release")
public class ConfigReleaseEntity extends BaseEntity<Long> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "release_id")
    private Long releaseId;

    @Column(name = "app_id", nullable = false)
    private Long appId;

    @Column(name = "config_ids", length = 2048)
    private String configIds;

    @Column(name = "release_version", length = 64)
    private String releaseVersion;

    @Column(name = "release_note", length = 1024)
    private String releaseNote;

    @Column(name = "status", length = 32)
    private String status = "published";

    @Column(name = "tenant_id")
    private String tenantId;

    public ConfigReleaseEntity() {
    }

    @Override
    public Long getId() {
        return releaseId;
    }

    public Long getReleaseId() {
        return releaseId;
    }

    public void setReleaseId(Long releaseId) {
        this.releaseId = releaseId;
    }

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

    public String getConfigIds() {
        return configIds;
    }

    public void setConfigIds(String configIds) {
        this.configIds = configIds;
    }

    public String getReleaseVersion() {
        return releaseVersion;
    }

    public void setReleaseVersion(String releaseVersion) {
        this.releaseVersion = releaseVersion;
    }

    public String getReleaseNote() {
        return releaseNote;
    }

    public void setReleaseNote(String releaseNote) {
        this.releaseNote = releaseNote;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
