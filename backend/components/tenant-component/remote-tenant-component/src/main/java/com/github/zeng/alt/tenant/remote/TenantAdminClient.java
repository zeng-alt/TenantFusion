package com.github.zeng.alt.tenant.remote;

import com.github.zeng.alt.tenant.api.TenantMetadata;
import com.github.zeng.alt.tenant.remote.client.TenantAdminApi;
import feign.FeignException;

import java.util.List;

/**
 * admin 租户元数据客户端的容错外壳：Feign 调用 + 缺席归一。
 * <p>
 * 传输由 {@link TenantAdminApi}（OpenFeign）完成，鉴权由
 * {@code TenantFeignConfiguration} 里的透传拦截器解决，端点本身保持鉴权。
 * 404 视为租户不存在，返回 null/空列表，其余异常直接抛出，由上层按 fail-fast 处理。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
public class TenantAdminClient implements TenantRemoteSource {

    private final TenantAdminApi api;

    public TenantAdminClient(TenantAdminApi api) {
        this.api = api;
    }

    @Override
    public TenantMetadata findById(String tenantId) {
        try {
            return api.findById(tenantId);
        } catch (FeignException.NotFound e) {
            return null;
        }
    }

    @Override
    public List<TenantMetadata> findAll() {
        try {
            List<TenantMetadata> result = api.findAll();
            return result == null ? List.of() : result;
        } catch (FeignException.NotFound e) {
            return List.of();
        }
    }
}
