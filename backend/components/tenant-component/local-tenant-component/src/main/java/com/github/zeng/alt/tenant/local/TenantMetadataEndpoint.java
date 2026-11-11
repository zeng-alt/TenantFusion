package com.github.zeng.alt.tenant.local;

import com.github.zeng.alt.tenant.api.TenantMetadata;
import com.github.zeng.alt.tenant.api.TenantMetadataProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 租户元数据 HTTP 端点：给 {@code remote-tenant-component} 远程读取。
 * <p>
 * 只返回启用中的租户（底层的 {@link TenantMetadataProvider} 已过滤停用/删除）。
 * 返回裸 {@link TenantMetadata} 而非 {@code RestResponse} 包裹——
 * {@code GlobalResponseAdvice} 只处理 {@code String} 约定，对象原样透传，
 * remote 侧可直接反序列化。查无时抛 404，恰好命中 remote 客户端的缺席处理。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
@RestController
@RequestMapping("${alt.tenant.store.path:/v1/tenants}")
public class TenantMetadataEndpoint {

    private final TenantMetadataProvider provider;

    public TenantMetadataEndpoint(TenantMetadataProvider provider) {
        this.provider = provider;
    }

    /**
     * 全量启用租户，供 remote 启动校验与预热。
     *
     * @return 元数据列表（可能为空）
     */
    @GetMapping
    public List<TenantMetadata> listAll() {
        return provider.findAll();
    }

    /**
     * 单租户查询，供 remote 路由解析。
     * <p>
     * 用 {@code ResponseEntity} 原生表达 404：抛异常会被全局异常处理改写状态码，
     * 而 {@code HttpEntity} 会绕过 {@code GlobalResponseAdvice}、状态原样送达，
     * 恰好命中 remote 客户端的缺席处理。
     *
     * @param tenantId 租户标识
     * @return 元数据，缺席时 404 空体
     */
    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantMetadata> getById(@PathVariable("tenantId") String tenantId) {
        return provider.findById(tenantId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
