package com.github.zeng.alt.config.server.entity;

import com.github.zeng.alt.domain.base.BaseEntity;
import jakarta.persistence.*;

import java.io.Serial;
import java.time.LocalDateTime;

@Entity
@Table(name = "sys_config_client_instance")
public class ConfigClientInstanceEntity extends BaseEntity<Long> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "app_code", nullable = false, length = 64)
    private String appCode;

    @Column(name = "ip", length = 64)
    private String ip;

    @Column(name = "instance_id", length = 128)
    private String instanceId;

    @Column(name = "client_version", length = 32)
    private String clientVersion;

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    @Column(name = "status", length = 32)
    private String status = "ONLINE";

    @Column(name = "tenant_id")
    private String tenantId;

    public ConfigClientInstanceEntity() {
    }

    @Override
    public Long getId() {
        return id;
    }

    public Long getIdInstance() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }

    public LocalDateTime getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(LocalDateTime lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
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
