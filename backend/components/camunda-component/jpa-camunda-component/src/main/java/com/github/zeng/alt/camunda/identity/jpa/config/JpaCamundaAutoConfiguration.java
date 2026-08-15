package com.github.zeng.alt.camunda.identity.jpa.config;

import com.github.zeng.alt.camunda.identity.api.CamundaTenantSource;
import com.github.zeng.alt.camunda.identity.api.CamundaUserGroupSource;
import com.github.zeng.alt.camunda.identity.api.config.CamundaIdentityAutoConfiguration;
import com.github.zeng.alt.camunda.identity.jpa.entity.MainRoleEntity;
import com.github.zeng.alt.camunda.identity.jpa.entity.MainTenantEntity;
import com.github.zeng.alt.camunda.identity.jpa.entity.MainUserEntity;
import com.github.zeng.alt.camunda.identity.jpa.entity.MainUserRoleEntity;
import com.github.zeng.alt.camunda.identity.jpa.repository.MainRoleRepository;
import com.github.zeng.alt.camunda.identity.jpa.repository.MainTenantRepository;
import com.github.zeng.alt.camunda.identity.jpa.repository.MainUserRepository;
import com.github.zeng.alt.camunda.identity.jpa.repository.MainUserRoleRepository;
import com.github.zeng.alt.camunda.identity.jpa.service.JpaCamundaTenantSource;
import com.github.zeng.alt.camunda.identity.jpa.service.JpaCamundaUserGroupSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Camunda JPA 身份源自动配置。
 * <p>
 * 扫描 main_user/main_role/main_user_role/main_tenant 实体与仓库，并在没有其他 {@link CamundaUserGroupSource}
 * /{@link CamundaTenantSource} Bean 时提供默认的 JPA 实现。必须在 {@link CamundaIdentityAutoConfiguration}
 * 之前完成，以便其 {@code @ConditionalOnBean(CamundaUserGroupSource.class)} 条件能命中。
 */
@AutoConfiguration
@ConditionalOnClass(CamundaUserGroupSource.class)
@AutoConfigureBefore(CamundaIdentityAutoConfiguration.class)
@AutoConfigurationPackage(basePackageClasses = {
        MainUserEntity.class,
        MainRoleEntity.class,
        MainUserRoleEntity.class,
        MainTenantEntity.class,
        MainUserRepository.class,
        MainRoleRepository.class,
        MainUserRoleRepository.class,
        MainTenantRepository.class

})
public class JpaCamundaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CamundaUserGroupSource.class)
    public CamundaUserGroupSource jpaCamundaUserGroupSource(
            MainUserRepository userRepository,
            MainRoleRepository roleRepository,
            MainUserRoleRepository userRoleRepository) {
        return new JpaCamundaUserGroupSource(userRepository, roleRepository, userRoleRepository);
    }

    @Bean
    @ConditionalOnMissingBean(CamundaTenantSource.class)
    public CamundaTenantSource jpaCamundaTenantSource(
            MainTenantRepository tenantRepository,
            MainUserRepository userRepository) {
        return new JpaCamundaTenantSource(tenantRepository, userRepository);
    }
}
