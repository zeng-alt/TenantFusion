package com.github.zeng.alt.tenant.remote.client;

import com.github.zeng.alt.tenant.api.TenantMetadata;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * admin 租户元数据端点的 Feign 声明（对端是
 * {@code local-tenant-component} 的 {@code TenantMetadataEndpoint}）。
 * <p>
 * 名称固定为 {@code tenant-admin-api}，超时按标准键配置：
 * {@code feign.client.config.tenant-admin-api.connectTimeout/readTimeout}。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
@FeignClient(
        name = "tenant-admin-api",
        url = "${alt.tenant.remote.base-url:http://127.0.0.1:8080}",
        path = "${alt.tenant.remote.path-prefix:/v1/tenants}",
        configuration = TenantFeignConfiguration.class)
public interface TenantAdminApi {

    /**
     * 单租户查询，缺席时对端返回 404。
     *
     * @param tenantId 租户标识
     * @return 元数据
     */
    @GetMapping("/{tenantId}")
    TenantMetadata findById(@PathVariable("tenantId") String tenantId);

    /**
     * 全量启用租户，用于启动校验与预热。
     *
     * @return 元数据列表
     */
    @GetMapping
    List<TenantMetadata> findAll();
}
