package com.github.zeng.alt.domain.config;

import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

/**
 * @author zengJiaJun
 * @since 2026年06月01日
 * @version 1.0
 */
@AutoConfiguration
public class DomainAutoConfiguration {

    @Bean
    @Order(Integer.MIN_VALUE)
    @ConditionalOnMissingBean
    public HibernatePropertiesCustomizer tenantColumnHibernatePropertiesCustomizer(ObjectProvider<CurrentTenantIdentifierResolver<String>> provider) {
        return hibernateProperties ->
                hibernateProperties
                        .put(
                                MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER,
                                provider.orderedStream()
                                        .findFirst()
                                        .orElse(new CurrentTenantIdentifierResolver<>() {

                                            @Override
                                            public String resolveCurrentTenantIdentifier() {
                                                return "master";
                                            }

                                            @Override
                                            public boolean validateExistingCurrentSessions() {
                                                return false;
                                            }
                                        })
                        );
    }
}
