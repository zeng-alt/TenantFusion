package com.github.zeng.alt.tenant.remote;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 远程租户源配置：经 OpenFeign 从 admin 侧读取租户元数据。
 * <p>
 * 给没有租户库直连权限的应用（如 workflow）使用，与
 * {@code local-tenant-component} 提供同一对 SPI，有 local 时自动退让。
 * 端点保持鉴权（不进白名单），鉴权由 Feign 拦截器透传解决。
 * <p>
 * 超时走标准 Feign 配置：
 * {@code feign.client.config.tenant-admin-api.connectTimeout/readTimeout}。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
@Data
@ConfigurationProperties(prefix = "alt.tenant.remote")
public class RemoteTenantProperties {

    /** 总开关，workflow 侧置 true 启用远程租户源 */
    private boolean enabled = false;

    /** admin 服务基地址 */
    private String baseUrl = "http://127.0.0.1:8080";

    /** 租户元数据接口前缀，必须与 admin 侧 local 模块的 alt.tenant.store.path 对齐 */
    private String pathPrefix = "/v1/tenants";

    /**
     * 透传的租户头名，必须与 core 的 {@code alt.tenant.tenant-header} 对齐。
     */
    private String tenantHeader = "X-Tenant-Id";

    /**
     * 服务间调用密钥（M2M 静态 key）：无用户 JWT 时（定时任务、启动预热），
     * 经 {@code X-Tenant-Service-Key} 头发送。必须与 admin 侧
     * {@code alt.tenant.store.service-key} 一致，生产经环境变量注入。
     * 为空时该通道关闭，无上下文调用直接 401。
     */
    private String serviceToken;

    /** 单租户查询缓存有效期，admin 侧改租户后最迟延迟该时长可见 */
    private Duration cacheTtl = Duration.ofMinutes(10);
}
