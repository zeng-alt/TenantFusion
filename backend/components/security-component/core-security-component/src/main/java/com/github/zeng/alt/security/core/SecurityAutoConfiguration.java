package com.github.zeng.alt.security.core;


import com.github.zeng.alt.security.api.AuthHelper;
import com.github.zeng.alt.security.api.AuthorityResolver;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import com.github.zeng.alt.tenant.api.TenantMetadataProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author zengJiaJun
 * @crateTime 2024年10月07日 19:09
 * @version 1.0
 */
@AutoConfiguration
@EnableConfigurationProperties(SecurityProperties.class)
@Import(WhiteListAutoConfiguration.class)
public class SecurityAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public TenantSwitchTemplate tenantSwitchTemplate(
			SecurityProperties properties, ObjectProvider<TenantMetadataProvider> providers) {
		return new TenantSwitchTemplate(properties, providers);
	}

	@Bean
	@ConditionalOnMissingBean(PasswordEncoder.class)
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	@ConditionalOnMissingBean
	public AuthHelper authHelper(SecurityProperties securityProperties, ObjectProvider<AuthorityResolver> authorityResolvers) {
		return new AuthHelperImpl(securityProperties, authorityResolvers);
	}

	@Bean
	@ConditionalOnMissingBean
	public ReactiveUserDetailsService reactiveUserDetailsService(PasswordEncoder passwordEncoder) {
		return username -> Mono.empty();
	}

	@Bean
	@ConditionalOnMissingBean
	public UserDetailsService inMemoryUserDetailsManager(PasswordEncoder passwordEncoder) {
		return new InMemoryUserDetailsManager();
	}


	@Bean
	@ConditionalOnMissingBean(AuthenticationEventPublisher.class)
	public AuthenticationEventPublisher defaultAuthenticationEventPublisher(
			ApplicationEventPublisher applicationEventPublisher) {
		return new DefaultAuthenticationEventPublisher(applicationEventPublisher);
	}

}
