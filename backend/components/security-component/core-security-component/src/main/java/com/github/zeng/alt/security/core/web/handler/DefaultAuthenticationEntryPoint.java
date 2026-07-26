package com.github.zeng.alt.security.core.web.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
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
@CommonsLog
@RequiredArgsConstructor
public class DefaultAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
			throws IOException {

		if (request.getRequestURI().equals("/error")) {
			ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
			problemDetail.setTitle("请求的资源不存在");
			String uri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
			problemDetail.setDetail("检查接口: " + uri);
			problemDetail.setInstance(URI.create(request.getRequestURI()));
			response.setStatus(HttpStatus.NOT_FOUND.value());
			response.setCharacterEncoding(StandardCharsets.UTF_8.name());
			response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
			objectMapper.writeValue(response.getWriter(), problemDetail);
			return;
		}

		ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
		problemDetail.setTitle("用户未登录或者Token无效/过期");
		problemDetail.setDetail(exception.getMessage());
		problemDetail.setInstance(URI.create(request.getRequestURI()));

		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(), problemDetail);
	}

}
