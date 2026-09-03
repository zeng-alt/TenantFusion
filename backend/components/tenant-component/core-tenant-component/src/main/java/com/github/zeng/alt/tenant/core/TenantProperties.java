package com.github.zeng.alt.tenant.core;

import com.github.zeng.alt.tenant.api.TenantMode;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 多租户全局配置。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
@Data
@ConfigurationProperties(prefix = "alt.tenant")
public class TenantProperties {

    /** 总开关。关闭时不注册任何 Hibernate 钩子，行为与未引入本组件一致 */
    private Boolean enabled = false;

    /**
     * 全局隔离预设。未引入 hybrid 模块时，所有租户都按这一档隔离；
     * 引入 hybrid 后，本值退化为「租户元数据未指定档位时的兜底」。
     */
    private TenantMode mode = TenantMode.NONE;

    /** 无租户上下文时（如白名单接口、定时任务）使用的租户标识 */
    private String defaultTenantId = "master";

    /** 超管租户标识，其请求绕过行级判别条件 */
    private String rootTenantId = "master";

    /** 从请求头解析租户的头名称，供服务间调用使用；为空则不启用 */
    private String tenantHeader = "X-Tenant-Id";

    /** 表级隔离时表名与后缀之间的分隔符 */
    private String tableSuffixSeparator = "_";

    /** 路由缓存有效期 */
    private Duration cacheTtl = Duration.ofMinutes(10);

    /** 元数据表配置 */
    private Metadata metadata = new Metadata();

    /** 库级隔离的数据源，键即 {@code TenantMetadata#dataSourceKey} */
    private Map<String, DataSourceConfig> datasources = new LinkedHashMap<>();

    @Data
    public static class Metadata {
        /** 是否从数据库表加载租户元数据；false 时仅使用全局配置 */
        private Boolean enabled = true;
        /** 元数据表名，该表必须位于未被路由的默认数据源 / 默认 schema */
        private String table = "main_tenant";
    }

    /**
     * 单个租户数据源。口令不落库、不写死在这里，通过
     * {@code ${TENANT_XXX_PASSWORD}} 之类的占位符从环境变量注入。
     */
    @Data
    public static class DataSourceConfig {
        private String url;
        private String username;
        private String password;
        private String driverClassName;
        /** 该数据源默认 schema，可与库级隔离叠加 */
        private String schema;
        private Integer maximumPoolSize = 5;
        private Integer minimumIdle = 0;
    }
}
