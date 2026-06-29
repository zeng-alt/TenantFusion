package com.github.zeng.alt.security.core.web.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;


/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年09月30日 20:06
 */
@RequiredArgsConstructor
public class DefaultAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
			throws IOException {

		ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
		problemDetail.setTitle("用户未登录或者Token无效/过期");
		problemDetail.setDetail(exception.getMessage());
		problemDetail.setInstance(URI.create(request.getRequestURI()));

		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(), problemDetail);
	}

}
