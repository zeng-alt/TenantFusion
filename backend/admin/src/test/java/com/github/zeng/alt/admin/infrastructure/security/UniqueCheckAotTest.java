package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.domain.config.DomainAutoConfiguration;
import jakarta.persistence.EntityManager;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.junit.jupiter.api.Test;
import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.javapoet.ClassName;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 使用实际 Bean 工厂元数据验证事务代理在 AOT 阶段生成，避免原生运行时再增强。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class UniqueCheckAotTest {
    @Test
    void shouldGenerateUniqueCheckTransactionProxyAheadOfTime() {
        try (final var context = new AnnotationConfigApplicationContext()) {
            context.register(TransactionConfiguration.class);
            final var repository = new RootBeanDefinition(DomainAutoConfiguration.class);
            repository.setFactoryMethodName("entityManagerUniqueCheckRepository");
            repository.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            context.registerBeanDefinition("entityManagerUniqueCheckRepository", repository);
            final var files = new InMemoryGeneratedFiles();
            final var generation = new DefaultGenerationContext(
                    new ClassNameGenerator(ClassName.get(UniqueCheckAotTest.class)), files);
            new ApplicationContextAotGenerator().processAheadOfTime(context, generation);
            generation.writeGeneratedContent();
            final String name = "entityManagerUniqueCheckRepository";
            final Class<?> targetType = context.getBeanFactory().getType(name);
            final Class<?> proxyType = context.getBean(AbstractAutoProxyCreator.class)
                    .determineBeanType(targetType, name);
            assertThat(proxyType.getName()).contains("EntityManagerUniqueCheckRepository$$SpringCGLIB");
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    static class TransactionConfiguration {
        @Bean
        EntityManager entityManager() {
            return mock(EntityManager.class);
        }
    }
}
