package com.github.zeng.alt.security.core.web.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/**
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年09月29日 21:36
 */
public class DefaultLoginFailureHandler implements AuthenticationFailureHandler {

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) {
//		String message = AuthenticationHelper.getErrorMsg(request).orElse(exception.getMessage());
//		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
//		problemDetail.setInstance(URI.create(request.getRequestURI()));
//		problemDetail.setTitle(message);
//
//		AuthenticationHelper.renderString(
//				response,
//				HttpStatus.BAD_REQUEST.value(),
//				message,
//				JsonUtils.toJsonString(problemDetail)
//		);
	}

}
