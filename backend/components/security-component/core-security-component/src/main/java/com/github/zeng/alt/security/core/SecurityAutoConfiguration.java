package com.github.zeng.alt.security.core;


import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.WhiteListService;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import com.github.zeng.alt.security.core.properties.WhiteListProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import java.util.List;

/**
 * @author zengJiaJun
 * @crateTime 2024年10月07日 19:09
 * @version 1.0
 */
@AutoConfiguration
@EnableConfigurationProperties({ WhiteListProperties.class, SecurityProperties.class })
public class SecurityAutoConfiguration {


	@Bean
	@ConditionalOnMissingBean(PasswordEncoder.class)
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	@ConditionalOnMissingBean
	public ReactiveUserDetailsService reactiveUserDetailsService(PasswordEncoder passwordEncoder) {
		UserDetails user = User
				.withUsername("root")
				.password(passwordEncoder.encode("123456"))
				.roles("ADMIN")
				.build();
		return new MapReactiveUserDetailsService(user);
	}

	@Bean
	@ConditionalOnMissingBean
	public UserDetailsService inMemoryUserDetailsManager(PasswordEncoder passwordEncoder) {
		return new InMemoryUserDetailsManager(
				List.of(SecurityUser.withUsername("root").password(passwordEncoder.encode("123456")).roles().build()));
	}

	@Bean
	@ConditionalOnMissingBean
	public WhiteListService defaultWhiteListService(WhiteListProperties whiteListProperties) {
		return whiteListProperties::getIgnoreUrl;
	}


	@Bean
	@ConditionalOnMissingBean(AuthenticationEventPublisher.class)
	public AuthenticationEventPublisher defaultAuthenticationEventPublisher(
			ApplicationEventPublisher applicationEventPublisher) {
		return new DefaultAuthenticationEventPublisher(applicationEventPublisher);
	}

}
