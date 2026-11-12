package com.github.zeng.alt.admin.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.admin.infrastructure.entity.User;
import com.github.zeng.alt.admin.infrastructure.repository.DepartmentRepository;
import com.github.zeng.alt.admin.infrastructure.repository.RoleRepository;
import com.github.zeng.alt.admin.infrastructure.repository.UserRepository;
import com.github.zeng.alt.admin.infrastructure.repository.UserRoleRepository;
import com.github.zeng.alt.admin.query.api.dto.PatchProfileDto;
import com.github.zeng.alt.admin.query.service.UserSecurityService;
import com.github.zeng.alt.admin.query.service.UserServiceImpl;
import com.github.zeng.alt.admin.query.service.transformation.UserDtoTransformation;
import com.github.zeng.alt.security.api.AuthHelper;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.WhiteListService;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import com.github.zeng.alt.security.core.web.config.WebSecurityAutoConfiguration;
import com.github.zeng.alt.security.jwt.JwtAuthAutoConfiguration;
import com.github.zeng.alt.security.jwt.JwtStorage;
import com.github.zeng.alt.security.jwt.JwtTokenProvider;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import com.github.zeng.alt.tenant.api.TenantSingleDataSourceProvider;
import io.vavr.control.Option;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 完整 admin 安全链中，停用账户不能凭旧 JWT 进入 authenticated 本人资料写入口。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class JwtAccountStatusTest {
    @AfterEach
    void clearContexts() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldAllowProfileWriteOnlyWhileDatabaseAccountIsEnabled(boolean enabled) {
        final var users = mock(UserRepository.class);
        final var user = new User();
        user.setUserId(7L);
        user.setUsername("review-user");
        user.setPassword("unused");
        user.setTenantBy("t001");
        user.setEnabled(enabled);
        when(users.findByUsername("review-user")).thenReturn(Option.of(user).toJavaOptional());
        when(users.findById(7L)).thenReturn(Option.of(user));
        final var service = new UserServiceImpl(
                users, mock(RoleRepository.class), mock(UserRoleRepository.class), mock(DepartmentRepository.class),
                new SecurityProperties(), mock(PasswordEncoder.class), Mappers.getMapper(UserDtoTransformation.class),
                mock(AuthHelper.class), mock(UserSecurityService.class));
        runner(users).run(context -> {
            assertThat(context).hasNotFailed();
            final String access = context.getBean(JwtTokenProvider.class).createToken(SecurityUser
                    .withUsername("review-user").id("7").tenant("t001").password("unused").roles().build());
            final var request = new MockHttpServletRequest("PATCH", "/v1/user/profile/7");
            request.setServletPath("/v1/user/profile/7");
            request.addHeader("Authorization", "Bearer " + access);
            final var response = new MockHttpServletResponse();
            final var reached = new AtomicBoolean();
            final var profile = new PatchProfileDto();
            profile.setNickName("updated");
            context.getBean("springSecurityFilterChain", Filter.class).doFilter(request, response, (req, res) -> {
                reached.set(true);
                assertThat(service.patchProfile(7L, profile).get()).isEqualTo(7L);
            });
            assertThat(reached.get()).isEqualTo(enabled);
            if (enabled) {
                verify(users).save(user);
                assertThat(user.getNickName()).isEqualTo("updated");
                return;
            }
            assertThat(response.getStatus()).isEqualTo(401);
            verify(users, never()).save(user);
        });
    }

    private WebApplicationContextRunner runner(UserRepository users) {
        final byte[] secret = new byte[192];
        new SecureRandom().nextBytes(secret);
        final var storage = mock(JwtStorage.class);
        when(storage.hasToken(anyString())).thenReturn(true);
        return new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                        WebSecurityAutoConfiguration.class, JwtAuthAutoConfiguration.class))
                .withUserConfiguration(MySecurityConfig.class)
                .withPropertyValues("security.username-login.enabled=false", "security.jwt-auth.authentication=true",
                        "security.jwt-auth.validation=true",
                        "security.jwt-auth.base64-secret=" + Base64.getEncoder().encodeToString(secret))
                .withBean(SecurityProperties.class, SecurityProperties::new)
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(JwtStorage.class, () -> storage)
                .withBean(AuthenticationManager.class, () -> authentication -> authentication)
                .withBean(UserDetailsService.class, () -> new UserDetailsServiceImpl(users,
                        new StaticListableBeanFactory().getBeanProvider(TenantSingleDataSourceProvider.class)))
                .withBean(WhiteListService.class, () -> Set::of)
                .withBean("mvcHandlerMappingIntrospector", HandlerMappingIntrospector.class,
                        HandlerMappingIntrospector::new);
    }
}
