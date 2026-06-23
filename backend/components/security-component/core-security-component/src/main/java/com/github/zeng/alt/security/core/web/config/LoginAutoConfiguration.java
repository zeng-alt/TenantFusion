package com.github.zeng.alt.security.core.web.config;

import com.github.zeng.alt.security.core.properties.LogoutProperties;
import com.github.zeng.alt.security.core.properties.UsernameLoginProperties;
import com.github.zeng.alt.security.core.web.handler.DefaultLoginFailureHandler;
import com.github.zeng.alt.security.core.web.SecurityBuilderCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import tools.jackson.databind.ObjectMapper;

/**
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年09月29日 20:01
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties({UsernameLoginProperties.class})
public class LoginAutoConfiguration {


	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "security.username-login.enabled", havingValue = "true")
	public DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(userDetailsService);
		daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);
		return daoAuthenticationProvider;
	}



	@Bean
	@ConditionalOnProperty(name = "security.username-login.enabled", havingValue = "true")
	public SecurityBuilderCustomizer initiateLoginCustomizer() {
		return http -> http
				.formLogin(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable);
	}

	@Bean
	@ConditionalOnMissingBean(AuthenticationFailureHandler.class)
	public AuthenticationFailureHandler loginFailureAuthenticationHandler(ObjectMapper objectMapper) {
		return new DefaultLoginFailureHandler(objectMapper);
	}

}
