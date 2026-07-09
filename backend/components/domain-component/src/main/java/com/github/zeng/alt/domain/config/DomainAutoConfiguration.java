package com.github.zeng.alt.domain.config;

import app.tozzi.model.input.JPASearchInput;
import com.github.zeng.alt.domain.key.IdGenerator;
import com.github.zeng.alt.domain.key.IdGeneratorProperties;
import com.github.zeng.alt.domain.sort.AutoSortRuntimeHints;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.annotation.Order;

/**
 * @author zengJiaJun
 * @since 2026年06月01日
 * @version 1.0
 */
@AutoConfiguration
@EnableConfigurationProperties(IdGeneratorProperties.class)
@ImportRuntimeHints(AutoSortRuntimeHints.class)
@RegisterReflectionForBinding({
        JPASearchInput.class,
        JPASearchInput.RootFilter.class,
        JPASearchInput.Filter.class,
        JPASearchInput.FieldFilter.class,
        JPASearchInput.FilterSingleValue.class,
        JPASearchInput.FilterMultipleValues.class,
        JPASearchInput.JPASearchOptions.class,
        JPASearchInput.JPASortOptions.class,
        JPASearchInput.JPASearchFilterOptions.class
})
public class DomainAutoConfiguration {

    @Bean
    public IdGenerator idGenerator(IdGeneratorProperties idGeneratorProperties) {
        return new IdGenerator(idGeneratorProperties);
    }

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
