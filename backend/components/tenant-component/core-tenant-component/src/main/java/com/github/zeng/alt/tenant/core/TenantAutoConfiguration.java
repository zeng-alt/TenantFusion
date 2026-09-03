package com.github.zeng.alt.tenant.core;

import com.github.zeng.alt.tenant.api.TenantAwareTaskDecorator;
import com.github.zeng.alt.tenant.api.TenantConnectionCustomizer;
import com.github.zeng.alt.tenant.api.TenantConnectionStrategy;
import com.github.zeng.alt.tenant.api.TenantDiscriminatorPolicy;
import com.github.zeng.alt.tenant.api.TenantMetadataProvider;
import com.github.zeng.alt.tenant.api.TenantRoutingRegistry;
import com.github.zeng.alt.tenant.api.TenantSqlRewriter;
import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.TaskDecorator;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

/**
 * 多租户核心自动配置：独占 Hibernate 的三个单值钩子并向下分发。
 * <p>
 * 通过 {@code alt.tenant.enabled=true} 启用。各隔离档位由是否引入对应策略模块决定，
 * 不在这里开关。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
@AutoConfiguration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@EnableConfigurationProperties(TenantProperties.class)
@ConditionalOnProperty(prefix = "alt.tenant", name = "enabled", havingValue = "true")
@CommonsLog
public class TenantAutoConfiguration {

    /**
     * 元数据来源。{@code alt.tenant.metadata.enabled=false} 时不注册，此时路由完全由配置决定。
     */
    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean(TenantMetadataProvider.class)
    @ConditionalOnProperty(prefix = "alt.tenant.metadata", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public JdbcTenantMetadataProvider jdbcTenantMetadataProvider(
            DataSource dataSource, TenantProperties properties) {
        return new JdbcTenantMetadataProvider(new JdbcTemplate(dataSource), properties);
    }

    /**
     * 单档位路由注册表。引入 hybrid 模块后会被按租户解析的实现顶掉。
     */
    @Bean
    @ConditionalOnMissingBean(TenantRoutingRegistry.class)
    public DefaultTenantRoutingRegistry tenantRoutingRegistry(
            TenantProperties properties,
            ObjectProvider<TenantMetadataProvider> metadataProvider) {
        return new DefaultTenantRoutingRegistry(properties, metadataProvider);
    }

    /**
     * 把注册表交给 Hibernate 内部那些拿不到容器的钩子。
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public TenantContextTrackerInitializer tenantContextTrackerInitializer(
            TenantRoutingRegistry registry) {
        TenantContextTracker.setRegistry(registry);
        return new TenantContextTrackerInitializer();
    }

    /**
     * Hibernate 唯一的租户标识解析器。也注册成 Bean，让既有的
     * {@code DomainAutoConfiguration} 的 customizer 取到同一个实例，避免两处装配打架。
     */
    @Bean
    @ConditionalOnMissingBean(CurrentTenantIdentifierResolver.class)
    public ContextTenantIdentifierResolver contextTenantIdentifierResolver(
            TenantProperties properties,
            ObjectProvider<TenantDiscriminatorPolicy> discriminatorPolicy) {
        return new ContextTenantIdentifierResolver(properties, discriminatorPolicy);
    }

    /** 兜底连接来源：主数据源 */
    @Bean
    @ConditionalOnBean(DataSource.class)
    public PrimaryDataSourceConnectionStrategy primaryDataSourceConnectionStrategy(
            DataSource dataSource) {
        return new PrimaryDataSourceConnectionStrategy(dataSource);
    }

    /**
     * 装配 Hibernate 的三个单值属性。
     * <p>
     * {@code @Order} 取 {@code MIN_VALUE + 1}，确保排在 {@code DomainAutoConfiguration} 那个
     * {@code MIN_VALUE} 的 customizer 之后执行，最终以本处的装配为准。
     */
    @Bean
    @ConditionalOnClass(name = "org.hibernate.SessionFactory")
    @Order(Integer.MIN_VALUE + 1)
    public HibernatePropertiesCustomizer tenantHibernatePropertiesCustomizer(
            TenantProperties properties,
            ContextTenantIdentifierResolver resolver,
            TenantRoutingRegistry registry,
            ObjectProvider<TenantConnectionStrategy> strategies,
            ObjectProvider<TenantConnectionCustomizer> customizers,
            ObjectProvider<TenantSqlRewriter> rewriters,
            ObjectProvider<DataSource> dataSource) {
        return hibernateProperties -> {
            hibernateProperties.put(
                    MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);

            List<TenantSqlRewriter> rewriterList = rewriters.orderedStream().toList();
            if (!rewriterList.isEmpty()) {
                hibernateProperties.put(
                        AvailableSettings.STATEMENT_INSPECTOR,
                        new CompositeTenantStatementInspector(rewriterList));
                log.info("多租户 SQL 重写已启用，重写器数量：" + rewriterList.size());
            }

            List<TenantConnectionStrategy> strategyList = strategies.orderedStream().toList();
            List<TenantConnectionCustomizer> customizerList = customizers.orderedStream().toList();
            // 只有确实需要在连接层做事时才注册 connection provider：
            // 纯行级 / 纯表级部署不注册，Hibernate 继续走普通 ConnectionProvider，零额外开销
            boolean needsConnectionRouting = !customizerList.isEmpty()
                    || strategyList.stream().anyMatch(s -> !(s instanceof PrimaryDataSourceConnectionStrategy));
            if (!needsConnectionRouting) {
                log.info("未引入模式级/库级隔离模块，跳过 MultiTenantConnectionProvider 注册");
                return;
            }
            DataSource primary = dataSource.getIfAvailable();
            if (primary == null) {
                throw new IllegalStateException(
                        "启用了连接层租户隔离但容器中没有 DataSource，无法注册 MultiTenantConnectionProvider");
            }
            hibernateProperties.put(
                    MultiTenancySettings.MULTI_TENANT_CONNECTION_PROVIDER,
                    new RoutingMultiTenantConnectionProvider(
                            registry, strategyList, customizerList,
                            primary, properties.getDefaultTenantId()));
            // 库级隔离下 Hibernate 启动做 schema 校验时还没有租户上下文，
            // 必须告诉它此刻该用哪个租户，否则启动阶段就取不到连接
            hibernateProperties.put(
                    MultiTenancySettings.TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY,
                    properties.getDefaultTenantId());
            log.info("多租户连接路由已启用，连接来源策略 " + strategyList.size()
                    + " 个，连接装饰器 " + customizerList.size() + " 个");
        };
    }

    /** 传播租户上下文到 {@code @Async} / 线程池任务 */
    @Bean
    @ConditionalOnMissingBean(TenantAwareTaskDecorator.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public TenantAwareTaskDecorator tenantAwareTaskDecorator() {
        return new TenantAwareTaskDecorator();
    }

    /**
     * 标记为 {@code @Primary}：容器里存在多个 {@code TaskDecorator} 时，
     * Spring Boot 的 {@code getIfUnique()} 会返回 null 导致全部失效。
     */
    @Bean
    @Primary
    public CompositeTaskDecorator compositeTaskDecorator(ObjectProvider<TaskDecorator> provider) {
        return new CompositeTaskDecorator(provider);
    }

    /** 请求头兜底 + ThreadLocal 清理 */
    @Bean
    @ConditionalOnClass(name = "jakarta.servlet.Filter")
    public FilterRegistrationBean<TenantResolveFilter> tenantResolveFilterRegistration(
            TenantProperties properties) {
        FilterRegistrationBean<TenantResolveFilter> registration =
                new FilterRegistrationBean<>(new TenantResolveFilter(properties));
        registration.setOrder(TenantResolveFilter.ORDER);
        return registration;
    }

    /** 仅用于承载启动期的静态注入，无行为 */
    public static class TenantContextTrackerInitializer {
    }
}
