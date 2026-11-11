package com.github.zeng.alt.tenant.remote;

import com.github.zeng.alt.tenant.api.TenantMetadata;

import java.util.List;

/**
 * 远程租户源：HTTP 调用与缓存解耦的接缝。
 * <p>
 * 生产实现为 {@link TenantAdminClient}（WebClient 调 admin 侧
 * {@code local-tenant-component} 暴露的 {@code TenantMetadataEndpoint}，
 * 线上传输类型即 {@link TenantMetadata} 本身），
 * 单测可用内存桩替换，无需起 HTTP 服务。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
public interface TenantRemoteSource {

    /**
     * 按租户标识查询，不存在时返回 null。
     *
     * @param tenantId 租户标识
     * @return 远端元数据
     */
    TenantMetadata findById(String tenantId);

    /**
     * 查询全部启用租户，用于启动期校验与预热。
     *
     * @return 远端元数据列表
     */
    List<TenantMetadata> findAll();
}
