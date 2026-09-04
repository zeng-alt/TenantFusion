package com.github.zeng.alt.api.tenant;

/**
 * 声明实体带有租户归属列。
 * <p>
 * 早期版本的两个方法带有默认实现——{@code getTenantBy()} 恒返回字符串 {@code "master"}、
 * {@code setTenantBy} 是空方法——而 {@code BaseEntity} 又实现了本接口，导致所有实体的租户归属
 * 都是假的：读永远得到 {@code "master"}，写永远被丢弃。这里改为抽象方法，
 * 实现者必须提供真实字段，避免再出现静默失效。
 * <p>
 * 实际实现见 {@code TenantBaseEntity}（row-tenant-component），它用 Hibernate 的
 * {@code @TenantId} 声明判别列。需要行级隔离的实体继承它即可，不必自行实现本接口。
 *
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年11月13日 10:31
 */
public interface TenantAuditable {

    /**
     * 返回该实体所属租户。
     *
     * @return 租户标识
     */
    String getTenantBy();

    /**
     * 设置该实体所属租户。
     *
     * @param tenantBy 租户标识
     */
    void setTenantBy(String tenantBy);
}
