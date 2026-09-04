package com.github.zeng.alt.tenant.liquibase;

import com.github.zeng.alt.tenant.api.TenantDialect;
import com.github.zeng.alt.tenant.api.TenantMetadataProvider;
import com.github.zeng.alt.tenant.api.TenantRoutingException;
import com.github.zeng.alt.tenant.api.TenantRoutingRegistry;
import com.github.zeng.alt.tenant.core.TenantAutoConfiguration;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;

/**
 * 多租户迁移自动配置。
 * <p>
 * 声明 {@code after = LiquibaseAutoConfiguration.class}，确保主迁移先跑完；
 * 同时通过 {@link EntityManagerFactoryDependsOnPostProcessor} 让 JPA 等本执行器结束，
 * 否则 Hibernate 的结构校验可能早于租户 schema 建好。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
@AutoConfiguration(after = {LiquibaseAutoConfiguration.class, TenantAutoConfiguration.class})
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@EnableConfigurationProperties(TenantLiquibaseProperties.class)
@ConditionalOnClass(SpringLiquibase.class)
@ConditionalOnProperty(prefix = "alt.tenant.liquibase", name = "enabled", havingValue = "true")
public class TenantLiquibaseAutoConfiguration {

    /** 执行器的 Bean 名，供依赖声明引用 */
    public static final String RUNNER_BEAN_NAME = "tenantLiquibaseRunner";

    /**
     * 兜底的数据源定位器：只在没有 {@code database-tenant-component} 时生效，
     * 遇到库级隔离的租户直接给出明确错误，而不是静默迁移到主库上。
     */
    @Bean
    @ConditionalOnMissingBean(TenantDataSourceLocator.class)
    public TenantDataSourceLocator tenantDataSourceLocator() {
        return key -> {
            throw new TenantRoutingException(
                    "租户数据源 [" + key + "] 无法定位：库级隔离的迁移需要引入 database-tenant-component");
        };
    }

    @Bean(RUNNER_BEAN_NAME)
    @ConditionalOnBean({DataSource.class, TenantMetadataProvider.class})
    @ConditionalOnMissingBean(TenantLiquibaseRunner.class)
    public TenantLiquibaseRunner tenantLiquibaseRunner(
            TenantLiquibaseProperties properties,
            TenantMetadataProvider metadataProvider,
            TenantRoutingRegistry registry,
            DataSource dataSource,
            ObjectProvider<TenantDialect> dialect,
            ResourceLoader resourceLoader,
            TenantDataSourceLocator dataSourceLocator) {
        return new TenantLiquibaseRunner(
                properties, metadataProvider, registry,
                dataSource, dialect, resourceLoader, dataSourceLocator);
    }

    /** 让 EntityManagerFactory 等租户迁移跑完再初始化 */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static EntityManagerFactoryDependsOnPostProcessor
            tenantLiquibaseEntityManagerFactoryDependsOnPostProcessor() {
        return new EntityManagerFactoryDependsOnPostProcessor(RUNNER_BEAN_NAME) {
        };
    }
}
