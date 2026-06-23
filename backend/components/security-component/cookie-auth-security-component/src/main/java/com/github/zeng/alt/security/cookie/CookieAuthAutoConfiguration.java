package com.github.zeng.alt.security.cookie;

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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Cookie 认证自动配置.
 * <p>
 * 通过 {@code security.cookie-auth.enabled=true} 启用 Cookie 认证，使用
 * {@link UsernameLoginProperties} 配置登录路径和参数名，
 * {@link LogoutProperties} 配置退出路径。
 * <p>
 * 认证原理：登录成功后在缓存中创建会话，将 sessionId 写入 Cookie；
 * 每次请求从 Cookie 中读取 sessionId 并查询缓存验证身份。
 *
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年10月07日
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties({CookieAuthProperties.class})
public class CookieAuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "security.cookie-auth", name = "validation", havingValue = "true")
    public SessionManager sessionManager(StorageTemplate storageTemplate, CookieAuthProperties cookieAuthProperties) {
        return new SessionManager(storageTemplate, cookieAuthProperties.getExpiration());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "security.cookie-auth", name = "authentication", havingValue = "true")
    public CookieAuthenticationSuccessHandler cookieAuthenticationSuccessHandler(
            SessionManager sessionManager,
            ObjectMapper objectMapper,
            CookieAuthProperties cookieAuthProperties) {
        return new CookieAuthenticationSuccessHandler(sessionManager, objectMapper, cookieAuthProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "security.cookie-auth", name = "authentication", havingValue = "true")
    public CookieAuthenticationFailureHandler cookieAuthenticationFailureHandler(ObjectMapper objectMapper) {
        return new CookieAuthenticationFailureHandler(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "security.cookie-auth", name = "authentication", havingValue = "true")
    public CookieLogoutHandler cookieLogoutHandler(
            SessionManager sessionManager,
            CookieAuthProperties cookieAuthProperties) {
        return new CookieLogoutHandler(sessionManager, cookieAuthProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "security.cookie-auth", name = "authentication", havingValue = "true")
    public CookieLogoutSuccessHandler cookieLogoutSuccessHandler(ObjectMapper objectMapper) {
        return new CookieLogoutSuccessHandler(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "security.cookie-auth", name = "authentication", havingValue = "true")
    public CookieLoginHelper cookieLoginHelper(
            AuthenticationManager authenticationManager,
            SessionManager sessionManager,
            CookieAuthProperties cookieAuthProperties) {
        return new CookieLoginHelper(authenticationManager, sessionManager, cookieAuthProperties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "security.cookie-auth", name = "validation", havingValue = "true")
    public SecurityBuilderCustomizer authCookieSecurityCustomizer(CookieAuthProperties cookieAuthProperties, SessionManager sessionManager) {
        return http -> {
            // ===== Cookie 认证过滤器（读取 Session Cookie）=====
            CookieAuthenticationFilter authFilter = new CookieAuthenticationFilter(
                    sessionManager,
                    cookieAuthProperties.getCookieName(),
                    cookieAuthProperties.getLogin()
            );

            http
                    .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class);
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "security.cookie-auth", name = "authentication", havingValue = "true")
    public SecurityBuilderCustomizer cookieSecurityCustomizer(
            CookieAuthenticationSuccessHandler successHandler,
            CookieAuthenticationFailureHandler failureHandler,
            CookieLogoutHandler cookieLogoutHandler,
            CookieLogoutSuccessHandler cookieLogoutSuccessHandler,
            UsernameLoginProperties usernameLoginProperties,
            CookieAuthProperties cookieAuthProperties,
            ObjectProvider<ObjectMapper> objectMapperProvider,
            ObjectProvider<AuthenticationManager> authenticationManagerProvider) {

        return http -> {
            ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
            AuthenticationManager authenticationManager = authenticationManagerProvider.getIfAvailable();
            if (authenticationManager == null) {
                throw new IllegalStateException(
                        "AuthenticationManager is required for Cookie authentication. " +
                        "Ensure WebSecurityAutoConfiguration is active.");
            }

            // ===== Cookie 登录过滤器 =====
            CookieLoginFilter loginFilter = new CookieLoginFilter(objectMapper);
            LoginProperties login = cookieAuthProperties.getLogin();
            loginFilter.setRequiresAuthenticationRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher(login.getMethod(), login.getLoginPath()));
            loginFilter.setUsernameParameter(usernameLoginProperties.getUsernameParameter());
            loginFilter.setPasswordParameter(usernameLoginProperties.getPasswordParameter());
            loginFilter.setAuthenticationSuccessHandler(successHandler);
            loginFilter.setAuthenticationFailureHandler(failureHandler);
            loginFilter.setAuthenticationManager(authenticationManager);



            // ===== Cookie 登出过滤器 =====
            LogoutFilter logoutFilter = new LogoutFilter(cookieLogoutSuccessHandler, cookieLogoutHandler);
            logoutFilter.setLogoutRequestMatcher(
                    PathPatternRequestMatcher.withDefaults().matcher(cookieAuthProperties.getLogout().getMethod(), cookieAuthProperties.getLogout().getLogoutPath())
            );

            http
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
        public OpenApiCustomizer loginApiCustomizer(UsernameLoginProperties usernameLoginProperties, CookieAuthProperties cookieAuthProperties) {
            return openApi -> {

                Schema<?> loginRequest = new ObjectSchema()
                        .addProperty(usernameLoginProperties.getUsernameParameter(), new StringSchema())
                        .addProperty(usernameLoginProperties.getPasswordParameter(), new StringSchema());

                Operation operation = new Operation()
                        .summary("jwt用户登录")
                        .description("通过用户名密码登录，返回 JWT")
                        .requestBody(new RequestBody()
                                .required(true)
                                .content(new Content()
                                        .addMediaType(
                                                "loginBody",
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

                openApi.path(cookieAuthProperties.getLogin().getLoginPath(), pathItem);
            };
        }

    }
}
