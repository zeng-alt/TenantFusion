package com.github.zeng.alt.security.core.web.handler;

import com.github.zeng.alt.api.rest.ErrorResponseEntity;
import com.github.zeng.alt.api.rest.RestResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年09月29日 21:36
 */
@RequiredArgsConstructor
public class DefaultLoginFailureHandler implements AuthenticationFailureHandler {

	private final ObjectMapper objectMapper;

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException {

		RestResponse<Object> restResponse = RestResponse.status(HttpStatus.UNAUTHORIZED.value()).message(exception.getMessage()).addError(exception);
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(), restResponse);
	}

}
