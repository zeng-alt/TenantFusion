package com.github.zeng.alt.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.security.core.properties.LoginProperties;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import java.util.List;

@AutoConfiguration
@ImportRuntimeHints(JjwtHints.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties({JwtProperties.class})
public class JwtAuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtTokenProvider jwtTokenProvider(JwtProperties jwtProperties) {
        return new JwtTokenProvider(jwtProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtStorage jwtStorage(JwtProperties jwtProperties, StorageTemplate storageTemplate) {
        return new JwtStorage(jwtProperties, storageTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "security.jwt-auth", name = "authentication", havingValue = "true")
    public JwtAuthenticationSuccessHandler jwtAuthenticationSuccessHandler(
            JwtTokenProvider jwtTokenProvider,
            JwtStorage jwtStorage,
            ObjectProvider<ObjectMapper> objectMapperProvider,
            JwtProperties jwtProperties)
    {

        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new JwtAuthenticationSuccessHandler(jwtTokenProvider, jwtStorage, objectMapper, jwtProperties);
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
    public JwtLogoutHandler jwtLogoutHandler(JwtLoginHelper loginHelper) {
        return new JwtLogoutHandler(loginHelper);
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
            JwtStorage jwtStorage,
            JwtProperties jwtProperties) {

        return new JwtLoginHelper(
                authenticationManager, jwtTokenProvider, jwtStorage, jwtProperties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "security.jwt-auth", name = "validation", havingValue = "true")
    public SecurityBuilderCustomizer authCookieSecurityCustomizer(
            JwtTokenProvider jwtTokenProvider,
            JwtStorage jwtStorage,
            UserDetailsService userDetailsService,
            JwtProperties jwtProperties) {
        return http -> {
            JwtAuthenticationFilter authFilter = new JwtAuthenticationFilter(
                    jwtTokenProvider,
                    jwtStorage,
                    userDetailsService,
                    "Authorization",
                    jwtProperties.getLogin(),
                    jwtProperties.getNewAccessTokenHeader(),
                    jwtProperties.getRefreshCookieName()
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

            JwtLoginFilter loginFilter = new JwtLoginFilter(objectMapper);
            LoginProperties login = jwtProperties.getLogin();
            loginFilter.setRequiresAuthenticationRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher(login.getMethod(), login.getLoginPath()));
            loginFilter.setUsernameParameter(usernameLoginProperties.getUsernameParameter());
            loginFilter.setPasswordParameter(usernameLoginProperties.getPasswordParameter());
            loginFilter.setAuthenticationSuccessHandler(jwtSuccessHandler);
            loginFilter.setAuthenticationFailureHandler(jwtFailureHandler);
            loginFilter.setAuthenticationManager(authenticationManager);

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
