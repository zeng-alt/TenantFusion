package com.github.zeng.alt.security.data.jpa;

import com.github.zeng.alt.security.data.DataPermissionTemplate;
import com.github.zeng.alt.security.data.core.DataPermissionAutoConfiguration;
import com.github.zeng.alt.security.data.core.DataPermissionProperties;
import io.vavr.control.Try;
import jakarta.persistence.EntityManager;
import org.aopalliance.intercept.MethodInterceptor;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;

/**
 * 在核心权限门面和 JPA 就绪后装配受保护查询门面。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@ImportRuntimeHints(DataPermissionRuntimeHints.class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@AutoConfiguration(after = {DataPermissionAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class DataPermissionJpaAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(DataPermissionJpaTemplate.class)
    public DataPermissionJpaTemplate dataPermissionJpaTemplate(
            EntityManager entityManager,
            DataPermissionTemplate permissions,
            DataPermissionProperties properties
    ) {
        return new DataPermissionJpaTemplate(entityManager, permissions, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    DataPermissionMethodInterceptor dataPermissionMethodInterceptor(
            EntityManager entityManager,
            DataPermissionTemplate permissions,
            PlatformTransactionManager transactions
    ) {
        return new DataPermissionMethodInterceptor(entityManager, permissions, transactions);
    }

    @Bean
    @ConditionalOnMissingBean(name = "dataPermissionMethodAdvisor")
    public Advisor dataPermissionMethodAdvisor(ObjectProvider<DataPermissionMethodInterceptor> interceptors) {
        final var pointcut = new StaticMethodMatcherPointcut() {
            @Override
            public boolean matches(Method method, Class<?> target) {
                return DataPermissionMethodHelper.resolve(method, target).isDefined();
            }
        };
        final MethodInterceptor interceptor = invocation -> interceptors.getObject().invoke(invocation);
        final var advisor = new DefaultPointcutAdvisor(pointcut, interceptor);
        advisor.setOrder(Ordered.HIGHEST_PRECEDENCE + 200);
        return advisor;
    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public HibernatePropertiesCustomizer dataPermissionSqlGuard() {
        return properties -> {
            final Object configured = properties.get("hibernate.session_factory.statement_inspector");
            final StatementInspector delegate = configured == null ? null : inspector(configured);
            properties.put("hibernate.session_factory.statement_inspector", new DataPermissionStatementInspector(delegate));
        };
    }

    private StatementInspector inspector(Object configured) {
        if (configured instanceof StatementInspector instance) {
            return instance;
        }
        final Class<?> type = configured instanceof Class<?> selected ? selected : Try.of(() ->
                ClassUtils.forName(configured.toString(), ClassUtils.getDefaultClassLoader())).get();
        return (StatementInspector) BeanUtils.instantiateClass(type);
    }
}
