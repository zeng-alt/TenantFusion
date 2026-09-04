package com.github.zeng.alt.tenant.liquibase;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 多租户迁移配置。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
@Data
@ConfigurationProperties(prefix = "alt.tenant.liquibase")
public class TenantLiquibaseProperties {

    /** 是否在启动时为各租户执行迁移 */
    private Boolean enabled = false;

    /** changelog 位置，默认与 Spring 的主迁移共用同一份 */
    private String changeLog = "classpath:/db/changelog/db.changelog-master.yaml";

    /** Liquibase contexts，留空表示不限制 */
    private String contexts;

    /** Liquibase labels，留空表示不限制 */
    private String labels;

    /**
     * 模式级租户的 schema 不存在时是否自动创建。
     * <p>关闭时遇到缺失的 schema 直接失败，适合 schema 由 DBA 预先开好的环境。
     */
    private Boolean createSchema = true;

    /**
     * 某个租户迁移失败时是否中断启动。
     * <p>默认中断：让一个租户带着旧结构继续服务，比启动失败更危险。
     */
    private Boolean failFast = true;

    /**
     * 传给 changelog 的 {@code tenantName} 参数是否用租户标识。
     * <p>既有 changelog 里的种子数据用 {@code ${tenantName}} 标记归属，
     * 逐租户迁移时应当替换成各自的租户标识。
     */
    private Boolean passTenantNameParameter = true;
}
