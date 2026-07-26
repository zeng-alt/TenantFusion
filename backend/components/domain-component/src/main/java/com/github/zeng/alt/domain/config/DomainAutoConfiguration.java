package com.github.zeng.alt.domain.config;

import app.tozzi.model.input.JPASearchInput;
import com.github.zeng.alt.domain.advice.DomainExceptionAdvice;
import com.github.zeng.alt.domain.key.IdGenerator;
import com.github.zeng.alt.domain.key.IdGeneratorProperties;
import com.github.zeng.alt.domain.sort.AutoSortRuntimeHints;
import com.github.zeng.alt.domain.validation.EntityManagerUniqueCheckRepository;
import com.github.zeng.alt.domain.validation.IUniqueCheckRepository;
import com.github.zeng.alt.domain.validation.ValidationRuntimeHints;
import com.github.zeng.alt.domain.validation.UniqueCheckServiceHolder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * @author zengJiaJun
 * @since 2026年06月01日
 * @version 1.0
 */
@AutoConfiguration
@EnableJpaAuditing
@EnableConfigurationProperties(IdGeneratorProperties.class)
@ImportRuntimeHints({AutoSortRuntimeHints.class, ValidationRuntimeHints.class})
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
    public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
        return new JPAQueryFactory(entityManager);
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


    @Order(Ordered.HIGHEST_PRECEDENCE)
    @Bean
    public DomainExceptionAdvice domainExceptionAdvice() {
        return new DomainExceptionAdvice();
    }


    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static UniqueCheckServiceHolder uniqueCheckServiceHolder(ObjectProvider<IUniqueCheckRepository> provider) {
        provider.ifAvailable(UniqueCheckServiceHolder::setRepository);
        return new UniqueCheckServiceHolder();
    }

    @Bean
    @ConditionalOnMissingBean(IUniqueCheckRepository.class)
    @ConditionalOnClass(name = "jakarta.persistence.EntityManager")
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static IUniqueCheckRepository entityManagerUniqueCheckRepository(EntityManager entityManager) {
        return new EntityManagerUniqueCheckRepository(entityManager);
    }
}
