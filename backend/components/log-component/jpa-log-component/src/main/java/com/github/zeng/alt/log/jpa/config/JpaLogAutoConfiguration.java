package com.github.zeng.alt.log.jpa.config;

import com.github.zeng.alt.log.jpa.entity.LogEntity;
import com.github.zeng.alt.log.jpa.event.LogEventPersistenceListener;
import com.github.zeng.alt.log.jpa.repository.LogRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA 日志模块自动配置。
 * <p>
 * 自动装配 {@link LogRepository} 和 {@link LogEventPersistenceListener}，
 * 将操作日志持久化到数据库。
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
@AutoConfiguration
@ConditionalOnClass(name = "jakarta.persistence.EntityManager")
@ConditionalOnBean(jakarta.persistence.EntityManagerFactory.class)
@AutoConfigurationPackage(basePackageClasses = {LogEntity.class, LogRepository.class})
public class JpaLogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LogEventPersistenceListener logEventPersistenceListener(LogRepository repository) {
        return new LogEventPersistenceListener(repository);
    }
}
