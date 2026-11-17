package com.github.zeng.alt.log.jpa.config;

import com.github.zeng.alt.log.jpa.entity.LogEntity;
import com.github.zeng.alt.log.jpa.event.LogEventPersistenceListener;
import com.github.zeng.alt.log.jpa.event.LoginLogEventPersistenceListener;
import com.github.zeng.alt.log.jpa.mapper.JpaLogMapper;
import com.github.zeng.alt.log.jpa.mapper.JpaLogMapperImpl;
import com.github.zeng.alt.log.jpa.repository.LogRepository;
import com.github.zeng.alt.log.jpa.repository.LoginLogRepository;
import com.github.zeng.alt.log.jpa.service.LogPersistenceService;
import com.github.zeng.alt.log.jpa.service.LogPersistenceServiceImpl;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;

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
@AutoConfigureAfter(HibernateJpaAutoConfiguration.class)
@ConditionalOnClass(EntityManagerFactory.class)
@AutoConfigurationPackage(basePackageClasses = {LogEntity.class, LogRepository.class})
public class JpaLogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JpaLogMapper jpaLogMapper() {
        return new JpaLogMapperImpl();
    }

    @Bean
    @ConditionalOnMissingBean(LogPersistenceService.class)
    public LogPersistenceService logPersistenceService(
            LogRepository operations,
            LoginLogRepository logins,
            JpaLogMapper mapper
    ) {
        return new LogPersistenceServiceImpl(operations, logins, mapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public LogEventPersistenceListener logEventPersistenceListener(LogPersistenceService persistence) {
        return new LogEventPersistenceListener(persistence);
    }

    @Bean
    @ConditionalOnMissingBean
    public LoginLogEventPersistenceListener loginLogEventPersistenceListener(LogPersistenceService persistence) {
        return new LoginLogEventPersistenceListener(persistence);
    }
}
