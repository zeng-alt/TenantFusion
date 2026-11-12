package com.github.zeng.alt.security.data.jpa;

import com.github.zeng.alt.security.data.DataPermissionTemplate;
import com.github.zeng.alt.security.data.core.DataPermissionAutoConfiguration;
import com.github.zeng.alt.security.data.core.DataPermissionProperties;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 在核心权限门面和 JPA 就绪后装配受保护查询门面。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
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
}
