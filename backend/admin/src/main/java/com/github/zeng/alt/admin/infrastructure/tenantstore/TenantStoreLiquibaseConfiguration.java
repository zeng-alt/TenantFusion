package com.github.zeng.alt.admin.infrastructure.tenantstore;

import com.github.zeng.alt.tenant.local.LocalTenantStoreAutoConfiguration;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 租户库 Liquibase 装配：在独立物理租户库上执行 {@code db.changelog-tenant.yaml}。
 * <p>
 * 业务库的 changelog 不再建 {@code main_tenant} 表，两边通过不同的
 * changelog 主文件隔离。跟随 {@code spring.liquibase.enabled} 总开关——
 * 生产关闭时租户库的表结构由外部统一管理，本 Bean 自动跳过。
 * changelog 方言目录复用业务库的 {@code spring.liquibase.parameters.changelogDir}
 *（dev {@code h2} / prod {@code pg}），拼成 {@code tenant/h2} 这类子目录。
 * <p>
 * 刻意不用第二个 {@code SpringLiquibase} Bean：Boot 的
 * {@code LiquibaseAutoConfiguration} 是 {@code @ConditionalOnMissingBean}，
 * 多一个同类型 Bean 会把业务库的迁移顶掉。这里用普通 Runner 在内部
 * 编程式执行，对 Boot 的条件毫无影响。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
@Configuration
@ConditionalOnProperty(prefix = "alt.tenant.store", name = "url")
@ConditionalOnProperty(prefix = "spring.liquibase", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@ImportRuntimeHints(TenantStoreRuntimeHints.class)
public class TenantStoreLiquibaseConfiguration {

    @Bean
    public TenantStoreMigrationRunner tenantStoreMigrationRunner(
            @Qualifier(LocalTenantStoreAutoConfiguration.TENANT_STORE_DATA_SOURCE)
            DataSource tenantStoreDataSource,
            ResourceLoader resourceLoader,
            @Value("${spring.liquibase.contexts:}") String contexts,
            @Value("${spring.liquibase.parameters.changelogDir:h2}") String changelogDir) {
        return new TenantStoreMigrationRunner(tenantStoreDataSource, resourceLoader, contexts, changelogDir);
    }

    /**
     * 定序：租户业务迁移（tenantLiquibaseRunner，读租户库决定迁哪些 schema/库）
     * 必须等租户库建表（本文件的 runner）完成后才能跑，否则查 main_tenant 直接报错。
     * 跨模块加不上 {@code @DependsOn}，只能在这里以后处理器补依赖边。
     *
     * @return BeanFactory 后处理器
     */
    @Bean
    public static BeanFactoryPostProcessor tenantMigrationOrdering() {
        return beanFactory -> {
            if (!(beanFactory instanceof DefaultListableBeanFactory registry)) {
                return;
            }
            if (!registry.containsBeanDefinition("tenantLiquibaseRunner")
                    || !registry.containsBeanDefinition("tenantStoreMigrationRunner")) {
                return;
            }
            String[] dependsOn = registry.getBeanDefinition("tenantLiquibaseRunner").getDependsOn();
            if (dependsOn == null) {
                dependsOn = new String[0];
            }
            boolean present = false;
            for (String name : dependsOn) {
                if ("tenantStoreMigrationRunner".equals(name)) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                String[] merged = new String[dependsOn.length + 1];
                System.arraycopy(dependsOn, 0, merged, 0, dependsOn.length);
                merged[dependsOn.length] = "tenantStoreMigrationRunner";
                registry.getBeanDefinition("tenantLiquibaseRunner").setDependsOn(merged);
            }
        };
    }

    /**
     * 租户库迁移执行器：启动期跑一次，与业务库迁移相互独立。
     */
    public static class TenantStoreMigrationRunner implements InitializingBean {

        private final DataSource dataSource;
        private final ResourceLoader resourceLoader;
        private final String contexts;
        private final String changelogDir;

        public TenantStoreMigrationRunner(DataSource dataSource, ResourceLoader resourceLoader,
                                          String contexts, String changelogDir) {
            this.dataSource = dataSource;
            this.resourceLoader = resourceLoader;
            this.contexts = contexts;
            this.changelogDir = changelogDir;
        }

        @Override
        public void afterPropertiesSet() throws Exception {
            SpringLiquibase liquibase = new SpringLiquibase();
            liquibase.setResourceLoader(resourceLoader);
            liquibase.setDataSource(dataSource);
            liquibase.setChangeLog("classpath:db/changelog/db.changelog-tenant.yaml");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("tenantChangelogDir", "tenant/" + changelogDir);
            liquibase.setChangeLogParameters(parameters);
            if (StringUtils.hasText(contexts)) {
                liquibase.setContexts(contexts);
            }
            liquibase.afterPropertiesSet();
        }
    }
}
