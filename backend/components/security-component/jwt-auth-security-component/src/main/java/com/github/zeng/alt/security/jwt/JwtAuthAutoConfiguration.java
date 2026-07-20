package com.github.zeng.alt.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.security.core.properties.LoginProperties;
import com.github.zeng.alt.security.core.properties.LogoutProperties;
import com.github.zeng.alt.security.core.properties.UsernameLoginProperties;
import com.github.zeng.alt.security.core.web.SecurityBuilderCustomizer;
import com.github.zeng.alt.storage.StorageTemplate;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import java.util.List;

/**
 * JWT 认证自动配置.
 * <p>
 * 通过 {@code security.jwt.enabled=true} 启用 JWT 认证，使用
 * {@link UsernameLoginProperties} 配置登录路径和参数名，
 * {@link LogoutProperties} 配置退出路径。
 * <p>
 * 启用后自动注册：
 * <ul>
 *   <li>{@link JwtTokenProvider} — JWT 创建和验证</li>
 *   <li>{@link JwtAuthenticationSuccessHandler} — 登录成功生成 JWT 并写入缓存</li>
 *   <li>{@link JwtAuthenticationFailureHandler} — 登录失败返回 JSON</li>
 *   <li>{@link JwtLogoutHandler} — 登出时清除缓存中的 JWT</li>
 *   <li>{@link JwtLogoutSuccessHandler} — 登出成功返回 JSON</li>
 *   <li>{@link SecurityBuilderCustomizer} — 装配登录/登出过滤器链，设置无状态会话</li>
 * </ul>
 *
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年10月07日
 */
@AutoConfiguration
@ImportRuntimeHints(JjwtHints.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties({JwtProperties.class})
public class JwtAuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtTokenProvider jwtTokenProvider(JwtProperties jwtProperties) {
        return new JwtTokenProvider(jwtProperties.getBase64Secret(), jwtProperties.getExpiration());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "security.jwt-auth", name = "authentication", havingValue = "true")
    public JwtAuthenticationSuccessHandler jwtAuthenticationSuccessHandler(
            JwtTokenProvider jwtTokenProvider,
            StorageTemplate storageTemplate,
            ObjectProvider<ObjectMapper> objectMapperProvider,
            JwtProperties jwtProperties)
    {

        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new JwtAuthenticationSuccessHandler(jwtTokenProvider, storageTemplate, objectMapper, jwtProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "security.jwt-auth", name = "authentication", havingValue = "true")
    public JwtAuthenticationFailureHandler jwtAuthenticationFailureHandler(ObjectProvider<ObjectMapper> objectMapperProvider) {
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new JwtAuthenticationFailureHandler(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "security.jwt-auth", name = "authentication", havingValue = "true")
    public JwtLogoutHandler jwtLogoutHandler(JwtTokenProvider jwtTokenProvider, StorageTemplate storageTemplate) {
        return new JwtLogoutHandler(jwtTokenProvider, storageTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "security.jwt-auth", name = "authentication", havingValue = "true")
    public JwtLogoutSuccessHandler jwtLogoutSuccessHandler(ObjectProvider<ObjectMapper> objectMapperProvider) {
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new JwtLogoutSuccessHandler(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtLoginHelper jwtLoginHelper(
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            StorageTemplate storageTemplate,
            JwtProperties jwtProperties) {

        return new JwtLoginHelper(
                authenticationManager, jwtTokenProvider, storageTemplate,
                jwtProperties.getExpiration(), jwtProperties.getRememberMeExpiration());
    }

    @Bean
    @ConditionalOnProperty(prefix = "security.jwt-auth", name = "validation", havingValue = "true")
    public SecurityBuilderCustomizer authCookieSecurityCustomizer(JwtTokenProvider jwtTokenProvider, ObjectProvider<StorageTemplate> storageTemplateProvider, JwtProperties jwtProperties) {
        return http -> {
            StorageTemplate storageTemplate = storageTemplateProvider.getIfAvailable();
            // ===== JWT 认证过滤器（校验 Bearer token + 记住我无感续期）=====
            JwtAuthenticationFilter authFilter = new JwtAuthenticationFilter(
                    jwtTokenProvider,
                    storageTemplate,
                    "Authorization",
                    jwtProperties.getLogin(),
                    jwtProperties.getRefreshTokenHeader(),
                    jwtProperties.getNewAccessTokenHeader()
            );

            http.addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class);
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "security.jwt-auth", name = "authentication", havingValue = "true")
    public SecurityBuilderCustomizer jwtSecurityCustomizer(
            JwtProperties jwtProperties,
            JwtAuthenticationSuccessHandler jwtSuccessHandler,
            JwtAuthenticationFailureHandler jwtFailureHandler,
            JwtLogoutHandler jwtLogoutHandler,
            JwtLogoutSuccessHandler jwtLogoutSuccessHandler,
            UsernameLoginProperties usernameLoginProperties,

            ObjectProvider<ObjectMapper> objectMapperProvider,
            ObjectProvider<AuthenticationManager> authenticationManagerProvider) {

        return http -> {

            ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
            AuthenticationManager authenticationManager = authenticationManagerProvider.getIfAvailable();
            if (authenticationManager == null) {
                throw new IllegalStateException(
                        "AuthenticationManager is required for JWT authentication. " +
                        "Ensure WebSecurityAutoConfiguration is active.");
            }

            // ===== JWT 登录过滤器 =====
            JwtLoginFilter loginFilter = new JwtLoginFilter(objectMapper);
            LoginProperties login = jwtProperties.getLogin();
            loginFilter.setRequiresAuthenticationRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher(login.getMethod(), login.getLoginPath()));
            loginFilter.setUsernameParameter(usernameLoginProperties.getUsernameParameter());
            loginFilter.setPasswordParameter(usernameLoginProperties.getPasswordParameter());
            loginFilter.setAuthenticationSuccessHandler(jwtSuccessHandler);
            loginFilter.setAuthenticationFailureHandler(jwtFailureHandler);
            loginFilter.setAuthenticationManager(authenticationManager);



            // ===== JWT 登出过滤器 =====
            LogoutFilter logoutFilter = new LogoutFilter(jwtLogoutSuccessHandler, jwtLogoutHandler);
            logoutFilter.setLogoutRequestMatcher(
                    PathPatternRequestMatcher.withDefaults().matcher(jwtProperties.getLogout().getMethod(), jwtProperties.getLogout().getLogoutPath())
            );

            http
                    .sessionManagement(session ->
                            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class)
                    .addFilterBefore(logoutFilter, LogoutFilter.class);
        };
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(
            name = "org.springdoc.core.customizers.OpenApiCustomizer"
    )
    static class SpringDocConfiguration {


        @Bean
        @ConditionalOnProperty(prefix = "security.jwt-auth", name = "authentication", havingValue = "true")
        public OpenApiCustomizer loginApiCustomizer(UsernameLoginProperties usernameLoginProperties, JwtProperties jwtProperties) {
            return openApi -> {

                List<Tag> tags = openApi.getTags();
                if (tags == null || tags.stream().noneMatch(t -> "Login".equals(t.getName()))) {
                    openApi.addTagsItem(new Tag()
                            .name("login")
                            .description("认证接口"));
                }

                Schema<?> loginRequest = new ObjectSchema()
                        .addProperty(usernameLoginProperties.getUsernameParameter(), new StringSchema())
                        .addProperty(usernameLoginProperties.getPasswordParameter(), new StringSchema());

                Operation operation = new Operation()
                        .summary("jwt用户登录")
                        .description("通过用户名密码登录，返回 JWT")
                        .tags(java.util.List.of("login"))
                        .requestBody(new RequestBody()
                                .required(true)
                                .content(new Content()
                                        .addMediaType(
                                                org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                                                new MediaType().schema(loginRequest)
                                        )
                                ))
                        .responses(new ApiResponses()
                                .addApiResponse("200",
                                        new ApiResponse()
                                                .description("登录成功"))
                                .addApiResponse("401",
                                        new ApiResponse()
                                                .description("用户名或密码错误")));

                PathItem pathItem = new PathItem()
                        .post(operation);



                openApi.path(jwtProperties.getLogin().getLoginPath(), pathItem);

                operation = new Operation()
                        .summary("jwt用户登出")
                        .description("登出当前用户，清除 JWT 缓存")
                        .tags(java.util.List.of("login"))
                        .responses(new ApiResponses()
                                .addApiResponse("200",
                                        new ApiResponse()
                                                .description("登出成功"))
                                .addApiResponse("401",
                                        new ApiResponse()
                                                .description("未登录或 Token 已过期")));

                pathItem = new PathItem()
                        .post(operation);

                openApi.path(jwtProperties.getLogout().getLogoutPath(), pathItem);
            };
        }



    }


}
