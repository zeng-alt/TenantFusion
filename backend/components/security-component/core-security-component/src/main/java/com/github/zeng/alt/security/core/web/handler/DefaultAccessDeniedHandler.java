package com.github.zeng.alt.security.core.web.handler;


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年09月30日 20:15
 */
@RequiredArgsConstructor
public class DefaultAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
			throws IOException {

		ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
		problemDetail.setTitle("用户没有访问权限");
		problemDetail.setDetail(exception.getMessage());
		problemDetail.setInstance(URI.create(request.getRequestURI()));

		response.setStatus(HttpStatus.FORBIDDEN.value());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(), problemDetail);
	}

}
