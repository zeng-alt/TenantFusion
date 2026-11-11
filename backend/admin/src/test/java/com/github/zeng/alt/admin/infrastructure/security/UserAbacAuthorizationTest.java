package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.infrastructure.entity.User;
import com.github.zeng.alt.admin.infrastructure.repository.DepartmentRepository;
import com.github.zeng.alt.admin.infrastructure.repository.RoleRepository;
import com.github.zeng.alt.admin.infrastructure.repository.UserRepository;
import com.github.zeng.alt.admin.infrastructure.repository.UserRoleRepository;
import com.github.zeng.alt.admin.query.api.dto.PatchUserDto;
import com.github.zeng.alt.admin.query.service.UserServiceImpl;
import com.github.zeng.alt.admin.query.service.transformation.UserDtoTransformation;
import com.github.zeng.alt.security.abac.AbacPolicy;
import com.github.zeng.alt.security.abac.AbacTemplate;
import com.github.zeng.alt.security.abac.core.AbacProperties;
import com.github.zeng.alt.security.abac.core.AbacTemplateImpl;
import com.github.zeng.alt.security.abac.method.AbacAuthorizeAttributeRegistry;
import com.github.zeng.alt.security.abac.method.AbacEnvironmentProvider;
import com.github.zeng.alt.security.abac.method.AbacMethodAdvisor;
import com.github.zeng.alt.security.abac.method.AbacMethodInterceptor;
import com.github.zeng.alt.security.abac.method.AbacSubject;
import com.github.zeng.alt.security.abac.method.AbacSubjectProvider;
import com.github.zeng.alt.security.abac.spel.SpelAbacEvaluator;
import com.github.zeng.alt.security.api.AuthHelper;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceService;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 注解驱动授权闭环：@AbacAuthorize 声明 UserResourceResolver，经 AOP 代理在任何变更前完成属性授权。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@ExtendWith(MockitoExtension.class)
class UserAbacAuthorizationTest {
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private SecurityProperties securityProperties;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserDtoTransformation userDtoTransformation;
    @Mock private AuthHelper authHelper;
    @Mock private RbacResourceService rbacResourceService;
    @InjectMocks private UserServiceImpl service;

    @Test
    void shouldRejectBeforeMutatingUserRolesOrCaches() {
        final UserServiceImpl proxy = proxied(10L, 20L);

        assertThatThrownBy(() -> proxy.patchUser(42L, new PatchUserDto()))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(userDtoTransformation, userRoleRepository, roleRepository, rbacResourceService);
    }

    @Test
    void shouldUpdateAfterAuthorization() {
        final var user = new User();
        user.setUserId(42L);
        final var command = new PatchUserDto();
        when(userRepository.findById(42L)).thenReturn(Option.of(user));
        when(authHelper.isSuperAdmin(42L)).thenReturn(false);
        final UserServiceImpl proxy = proxied(10L, 10L);

        assertThat(proxy.patchUser(42L, command).get()).isEqualTo(42L);
        verify(userDtoTransformation).mergeEntity(command, user);
    }

    private UserServiceImpl proxied(long subjectDeptId, long resourceDeptId) {
        final AbacSubjectProvider subjectProvider = permission ->
                Either.right(new AbacSubject("t001", Map.of("deptId", subjectDeptId)));
        final AbacEnvironmentProvider environmentProvider = context -> Map.of();
        final UserRepository resourceUsers = mock(UserRepository.class);
        final User resourceUser = new User();
        resourceUser.setUserId(42L);
        resourceUser.setDeptId(resourceDeptId);
        resourceUser.setTenantBy("t001");
        when(resourceUsers.findById(42L)).thenReturn(Option.of(resourceUser));
        final UserResourceResolver resolver =
                new UserResourceResolver(resourceUsers, Mappers.getMapper(AbacUserMapper.class));
        final StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("userResourceResolver", resolver);
        final AbacTemplate template = new AbacTemplateImpl(
                context -> List.of(new AbacPolicy("1", "t001", "user:update",
                        "subject['deptId'] == resource['deptId']", true, Option.none())),
                new SpelAbacEvaluator(), new AbacProperties());
        final AbacMethodInterceptor interceptor = new AbacMethodInterceptor(
                new AbacAuthorizeAttributeRegistry(), subjectProvider, environmentProvider, beans, template);
        final ProxyFactory factory = new ProxyFactory(service);
        factory.setProxyTargetClass(true);
        factory.addAdvisor(new AbacMethodAdvisor(interceptor, 0));
        return (UserServiceImpl) factory.getProxy();
    }
}