package com.github.zeng.alt.tenant.row;

import com.github.zeng.alt.domain.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import org.springframework.lang.Nullable;

import java.io.Serial;
import java.io.Serializable;

/**
 * 参与行级隔离的实体基类。
 * <p>
 * 刻意不把租户列加进 {@link BaseEntity}——那会波及全部实体和对应的 40 张表。
 * 需要行级隔离的实体改继承本类，并给对应表补 {@code tenant_by} 列与索引即可。
 * <p>
 * {@code @TenantId} 由 Hibernate 原生支持：查询自动追加 {@code where tenant_by = ?}，
 * 插入时自动回填当前租户，取值来自 {@code CurrentTenantIdentifierResolver}。
 *
 * @param <PK> 主键类型
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
@Getter
@Setter
@MappedSuperclass
public abstract class TenantBaseEntity<PK extends Serializable> extends BaseEntity<PK> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 租户判别列。字段名与既有的 {@code DictData#tenantBy} / {@code DictType#tenantBy} 保持一致，
     * 便于那两个实体后续平滑改为继承本类。
     */
    @TenantId
    @Nullable
    @Column(name = "tenant_by", length = 64)
    private String tenantBy;
}
