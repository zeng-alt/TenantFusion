package com.github.zeng.alt.security.captcha;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.captcha.core.CaptchaTemplate;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;

@RequiredArgsConstructor
public class CaptchaAuthenticationFilter extends OncePerRequestFilter {

    private final CaptchaTemplate captchaTemplate;
    private final CaptchaAuthProperties properties;
    private final ObjectMapper objectMapper;
    private final RequestMatcher requestMatcher;

    public CaptchaAuthenticationFilter(CaptchaTemplate captchaTemplate,
                                       CaptchaAuthProperties properties,
                                       ObjectMapper objectMapper) {
        this.captchaTemplate = captchaTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.requestMatcher = PathPatternRequestMatcher.withDefaults()
                .matcher(properties.getMethod(), properties.getLoginPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!requestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String captchaKey = readCaptchaKeyFromCookie(request);
        String captchaCode = request.getParameter(properties.getCodeParameter());

        boolean verified = captchaTemplate.verify(captchaKey, captchaCode);

        if (!verified) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
            problemDetail.setTitle("验证码错误");
            problemDetail.setInstance(URI.create(request.getRequestURI()));
            objectMapper.writeValue(response.getWriter(), problemDetail);
            return;
        }

        captchaTemplate.deleteCookie(response, captchaKey);
        filterChain.doFilter(request, response);
    }

    private String readCaptchaKeyFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            String cookieName = properties.getCookieName();
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
