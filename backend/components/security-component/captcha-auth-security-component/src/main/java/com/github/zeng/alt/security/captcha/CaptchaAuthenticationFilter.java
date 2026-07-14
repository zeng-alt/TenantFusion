package com.github.zeng.alt.security.captcha;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.captcha.core.CaptchaTemplate;
import com.github.zeng.alt.log.BusinessStatus;
import com.github.zeng.alt.log.BusinessType;
import com.github.zeng.alt.log.Log;
import com.github.zeng.alt.log.OperLogEvent;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

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

        String captchaKey = request.getParameter(properties.getKeyParameter());
        String captchaCode = request.getParameter(properties.getCodeParameter());

        boolean verified = captchaTemplate.verify(captchaKey, captchaCode);

        if (!verified) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
            problemDetail.setTitle("用户未登录或者jwt无效/过期");
            problemDetail.setInstance(URI.create(request.getRequestURI()));
            objectMapper.writeValue(response.getWriter(), problemDetail);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
