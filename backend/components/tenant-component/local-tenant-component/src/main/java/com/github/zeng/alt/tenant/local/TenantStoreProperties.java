package com.github.zeng.alt.tenant.local;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 物理独立租户库的连接配置。
 * <p>
 * 租户库只装 {@code main_tenant} 元数据表，不进任何租户路由拦截器，
 * 避免「查元数据前先要知道租户」的死循环。口令不写死，通过
 * {@code ${TENANT_STORE_PASSWORD}} 之类的占位符从环境变量注入。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
@Data
@ConfigurationProperties(prefix = "alt.tenant.store")
public class TenantStoreProperties {

    /**
     * 租户库 JDBC URL。为空时本模块不生效（单库老部署零影响）。
     * 物理独立库，例如 {@code jdbc:postgresql://db:5432/tenant_store}，
     * 开发期可用独立 H2 {@code jdbc:h2:mem:tenant_db;DB_CLOSE_DELAY=-1}。
     */
    private String url;

    private String username;

    private String password;

    private String driverClassName;

    /** 元数据表名，位于租户库默认 schema */
    private String table = "main_tenant";

    /**
     * 元数据 HTTP 端点路径，remote-tenant-component 按此前缀调用。
     * 两边默认值对齐，自定义时必须两边一起改。
     */
    private String path = "/v1/tenants";

    /**
     * 服务间调用密钥（M2M）：workflow 这类无用户上下文的调用方
     * 经 {@code X-Tenant-Service-Key} 头携带，admin 侧仅放行元数据端点。
     * 为空时不启用该通道（只剩用户 JWT 一种方式）。
     * 生产必须经环境变量注入，dev 可用固定值联调。
     */
    private String serviceKey;

    private Integer maximumPoolSize = 5;

    private Integer minimumIdle = 0;
}
