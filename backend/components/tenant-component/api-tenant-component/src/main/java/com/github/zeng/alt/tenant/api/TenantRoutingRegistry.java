package com.github.zeng.alt.tenant.api;

/**
 * 租户路由解析入口，带缓存。
 * <p>
 * core 模块提供的默认实现从全局配置取隔离档位；引入 hybrid 模块后由按租户解析的实现接管。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
public interface TenantRoutingRegistry {

    /**
     * 解析租户路由。实现不得返回 null——无法识别的租户应回落到默认路由。
     *
     * @param tenantId 租户标识，可为 null（表示无租户上下文）
     * @return 路由结果
     */
    TenantRouting resolve(String tenantId);

    /**
     * 失效单个租户的缓存，在租户元数据被修改后调用。
     *
     * @param tenantId 租户标识
     */
    void evict(String tenantId);

    /** 失效全部缓存 */
    void evictAll();
}
