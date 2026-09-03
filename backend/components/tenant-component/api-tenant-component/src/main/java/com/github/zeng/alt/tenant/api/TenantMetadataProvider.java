package com.github.zeng.alt.tenant.api;

import java.util.List;
import java.util.Optional;

/**
 * 租户元数据来源 SPI。
 * <p>
 * 实现必须走 <b>未被路由</b> 的连接（默认数据源 / 默认 schema），否则「查租户元数据前先要知道租户」会形成死循环。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
public interface TenantMetadataProvider {

    /**
     * 按租户标识查询。
     *
     * @param tenantId 租户标识
     * @return 元数据，不存在或已停用时返回空
     */
    Optional<TenantMetadata> findById(String tenantId);

    /**
     * 查询全部启用中的租户，用于启动期校验与数据源预热。
     *
     * @return 元数据列表
     */
    List<TenantMetadata> findAll();
}
