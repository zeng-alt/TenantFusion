package com.github.zeng.alt.security.core.reactive.config;


import com.github.zeng.alt.security.api.ReactiveAuthorizationManagerProvider;
import com.github.zeng.alt.security.api.WhiteListService;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import com.github.zeng.alt.security.core.reactive.*;
import com.github.zeng.alt.security.core.reactive.handler.DefaultReactiveAccessDeniedHandler;
import com.github.zeng.alt.security.core.reactive.handler.DefaultReactiveAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthenticatedReactiveAuthorizationManager;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年09月29日 21:04
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@EnableWebFluxSecurity
@RequiredArgsConstructor
@ConditionalOnClass({ WebFilterChainProxy.class })
public class ReactiveSecurityAutoConfiguration {

	private final SecurityProperties securityProperties;

	@Bean
	public SecurityWebFilterChain springSecurityFilterChain(
			ServerHttpSecurity http,
			ObjectProvider<ReactiveAuthorizationManagerProvider<AuthorizationContext>> reactiveAuthorizationManagerProviders,
			ObjectProvider<ServerHttpSecurityBuilderCustomizer> customizers,
			WhiteListService whiteListService,
			ServerAccessDeniedHandler serverAccessDeniedHandler,
			ServerAuthenticationEntryPoint serverAuthenticationEntryPoint
	) {

		http.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable);
		http.csrf(ServerHttpSecurity.CsrfSpec::disable);
		http.formLogin(ServerHttpSecurity.FormLoginSpec::disable);
		http.logout(ServerHttpSecurity.LogoutSpec::disable);
		http.exceptionHandling(ex -> ex.authenticationEntryPoint(serverAuthenticationEntryPoint).accessDeniedHandler(serverAccessDeniedHandler));
		http.authorizeExchange(authorizeExchangeSpec -> {
			authorizeExchangeSpec
					.pathMatchers(HttpMethod.POST, "/login/**").permitAll();
			if (securityProperties.getEnabledAccess()) {
				authorizeExchangeSpec.anyExchange().access(compositeReactiveAuthorizationManager(reactiveAuthorizationManagerProviders, whiteListService));
			} else {
				authorizeExchangeSpec.anyExchange().permitAll();
			}
		});
		customizers.orderedStream().forEach(customizer -> customizer.customizer(http));
		return http.build();
	}

	private CompositeReactiveAuthorizationManager compositeReactiveAuthorizationManager(ObjectProvider<ReactiveAuthorizationManagerProvider<AuthorizationContext>> reactiveAuthorizationManagerProviders, WhiteListService whiteListService) {
		List<ReactiveAuthorizationManager<AuthorizationContext>> list = new ArrayList<>();
		list.add(ReactiveWhiteListAuthorizationManager.authenticated(whiteListService));
		List<ReactiveAuthorizationManager<AuthorizationContext>> managers = reactiveAuthorizationManagerProviders.orderedStream().map(ReactiveAuthorizationManagerProvider::get).toList();
		list.addAll(managers);
		if (CollectionUtils.isEmpty(managers)) {
			list.add(AuthenticatedReactiveAuthorizationManager.authenticated());
		}
		return new CompositeReactiveAuthorizationManager(list);
	}



	@Bean
	@ConditionalOnMissingBean
	public ServerAccessDeniedHandler serverAccessDeniedHandler() {
		return DefaultReactiveAccessDeniedHandler.handler();
	}

	@Bean
	@ConditionalOnMissingBean
	public ServerAuthenticationEntryPoint serverAuthenticationEntryPoint() {
		return DefaultReactiveAuthenticationEntryPoint.handler();
	}

}
