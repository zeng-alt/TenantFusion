package com.github.zeng.alt.lock.database;

import com.github.zeng.alt.domain.base.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * sys_distributed_lock 表实体
 */
@Entity
@Table(name = "sys_distributed_lock")
public class SysDistributedLock extends BaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 锁名称（唯一键）
     */
    @Column(name = "lock_name", nullable = false, unique = true, length = 128)
    private String lockName;

    /**
     * 当前持有锁的实例ID
     */
    @Column(name = "instance_id", nullable = false, length = 64)
    private String instanceId;

    /**
     * 加锁时间
     */
    @Column(name = "locked_at", nullable = false)
    private LocalDateTime lockedAt;

    /**
     * 过期时间（允许为空：表示永不过期锁）
     */
    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    public SysDistributedLock() {
    }

    public Long getId() {
        return id;
    }

    public String getLockName() {
        return lockName;
    }

    public void setLockName(String lockName) {
        this.lockName = lockName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }

    public LocalDateTime getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(LocalDateTime expireAt) {
        this.expireAt = expireAt;
    }
}