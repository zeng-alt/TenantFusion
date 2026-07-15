package com.github.zeng.alt.config.server.entity;

import com.github.zeng.alt.domain.base.BaseEntity;
import jakarta.persistence.*;

import java.io.Serial;

@Entity
@Table(name = "sys_config_info", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"app_id", "data_id", "group_name"})
})
public class ConfigInfoEntity extends BaseEntity<Long> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id")
    private Long configId;

    @Column(name = "app_id", nullable = false)
    private Long appId;

    @Column(name = "data_id", nullable = false, length = 255)
    private String dataId;

    @Column(name = "group_name", nullable = false, length = 128)
    private String groupName = "DEFAULT_GROUP";

    @Lob
    @Column(name = "content", columnDefinition = "CLOB")
    private String content;

    @Column(name = "format", length = 32)
    private String format = "properties";

    @Column(name = "description", length = 512)
    private String description;

    @Version
    @Column(name = "version")
    private Integer version = 0;

    @Column(name = "status", length = 32)
    private String status = "draft";

    @Column(name = "tenant_id")
    private String tenantId;

    public ConfigInfoEntity() {
    }

    @Override
    public Long getId() {
        return configId;
    }

    public Long getConfigId() {
        return configId;
    }

    public void setConfigId(Long configId) {
        this.configId = configId;
    }

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
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
