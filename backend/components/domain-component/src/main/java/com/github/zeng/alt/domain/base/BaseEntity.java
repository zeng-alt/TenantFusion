package com.github.zeng.alt.domain.base;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.querydsl.core.annotations.QueryTransient;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Auditable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.util.ProxyUtils;
import org.springframework.lang.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年10月30日 20:05
 */
@Getter
@MappedSuperclass
@EntityListeners({AuditingEntityListener.class})
/*
 * 有意不再实现 TenantAuditable：租户列只属于确实参与行级隔离的实体。
 * 让全部实体都带上它，只能靠恒返回 "master" 的假默认实现兜着，反而掩盖问题。
 * 需要行级隔离的实体请继承 TenantBaseEntity（row-tenant-component）。
 */
public abstract class BaseEntity<PK extends Serializable> implements Auditable<String, PK, LocalDateTime>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @CreatedBy
    @Nullable
    @Column(name = "created_by", updatable = false)
    private String createdBy;
    @CreatedDate
    @Nullable
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;
    @Nullable
    @LastModifiedBy
    @Column(name = "last_modified_by")
    private String lastModifiedBy;
    @LastModifiedDate
    @Nullable
    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    /**
     * Returns the user who created this entity.
     *
     * @return the createdBy
     */
    @QueryTransient
    @Override
    public Optional<String> getCreatedBy() {
        return Optional.ofNullable(this.createdBy);
    }

    /**
     * Returns the creation date of the entity.
     *
     * @return the createdDate
     */
    @Override
    @QueryTransient
    public Optional<LocalDateTime> getCreatedDate() {
        return this.createdDate == null ? Optional.empty() : Optional.of(this.createdDate);
    }


    /**
     * Returns the user who modified the entity lastly.
     *
     * @return the lastModifiedBy
     */
    @Override
    @QueryTransient
    public Optional<String> getLastModifiedBy() {
        return Optional.ofNullable(this.lastModifiedBy);
    }


    /**
     * Returns the date of the last modification.
     *
     * @return the lastModifiedDate
     */
    @Override
    @QueryTransient
    public Optional<LocalDateTime> getLastModifiedDate() {
        return this.lastModifiedDate == null ? Optional.empty() : Optional.of(this.lastModifiedDate);
    }

    @Override
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    @Override
    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    @Override
    @Transient
    @JsonIgnore
    public boolean isNew() {
        return getId() == null;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (this == obj) {
            return true;
        } else if (!this.getClass().equals(ProxyUtils.getUserClass(obj))) {
            return false;
        } else {
            BaseEntity<?> that = (BaseEntity)obj;
            return this.getId() == null ? false : this.getId().equals(that.getId());
        }
    }

    public int hashCode() {
        int hashCode = 17;
        hashCode += this.getId() == null ? 0 : this.getId().hashCode() * 31;
        return hashCode;
    }
}
