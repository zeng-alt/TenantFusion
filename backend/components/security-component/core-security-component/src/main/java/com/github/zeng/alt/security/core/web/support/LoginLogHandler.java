package com.github.zeng.alt.security.core.web.support;

import com.github.zeng.alt.log.LoginInfoEvent;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.core.model.LoginAttemptDto;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import io.vavr.control.Option;
import io.vavr.control.Try;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 为 HTTP 和编程式登录统一采集审计信息，在认证边界隔离日志故障。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@CommonsLog
@RequiredArgsConstructor
public class LoginLogHandler {
    public static final String USERNAME_ATTRIBUTE = "loginUsername";
    public static final String TENANT_ATTRIBUTE = "loginTenantId";

    private final ApplicationEventPublisher eventPublisher;

    /** 在认证可能切换或清理租户上下文之前保存本次请求的信息。 */
    public static void captureAttempt(HttpServletRequest request, String username) {
        request.setAttribute(USERNAME_ATTRIBUTE, Option.of(username).getOrElse(""));
        request.setAttribute(TENANT_ATTRIBUTE, Option.of(TenantContextHolder.getTenantId()).getOrElse(""));
    }

    /** 成功日志优先使用认证用户的租户，审计接收方故障不影响登录。 */
    public void success(HttpServletRequest request, SecurityUser user) {
        success(capture(request), user);
    }

    /** 失败日志使用请求快照，不读取已被认证过程修改的租户。 */
    public void failure(HttpServletRequest request, AuthenticationException exception) {
        failure(capture(request), exception);
    }

    /** 编程式登录保留原认证异常，同时发布一次成功或失败日志。 */
    public SecurityUser authenticate(AuthenticationManager manager, String username, String password) {
        final LoginAttemptDto attempt = currentAttempt(username);
        return Try.of(() -> manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(username, password)))
                .onFailure(AuthenticationException.class, exception -> failure(attempt, exception))
                .map(Authentication::getPrincipal)
                .map(SecurityUser.class::cast)
                .peek(user -> success(attempt, user))
                .get();
    }

    private void success(LoginAttemptDto attempt, SecurityUser user) {
        final String tenantId = Option.of(user.getTenantName()).filter(StringUtils::hasText)
                .getOrElse(attempt.tenantId());
        final LoginInfoEvent event = create(attempt);
        event.setUsername(user.getUsername());
        event.setTenantId(tenantId);
        event.setStatus("0");
        event.setMessage("登录成功");
        publish(event);
    }

    private void failure(LoginAttemptDto attempt, AuthenticationException exception) {
        final LoginInfoEvent event = create(attempt);
        event.setStatus("1");
        event.setMessage(exception.getMessage());
        publish(event);
    }

    private LoginInfoEvent create(LoginAttemptDto attempt) {
        final LoginInfoEvent event = new LoginInfoEvent();
        event.setUsername(attempt.username());
        event.setTenantId(attempt.tenantId());
        event.setIp(attempt.ip());
        event.setUserAgent(attempt.userAgent());
        event.setLoginTime(LocalDateTime.now());
        return event;
    }

    private void publish(LoginInfoEvent event) {
        Try.run(() -> eventPublisher.publishEvent(event))
                .onFailure(error -> log.error("Failed to publish login audit event", error));
    }

    private LoginAttemptDto capture(HttpServletRequest request) {
        final String username = Option.of(request.getAttribute(USERNAME_ATTRIBUTE)).map(String::valueOf)
                .getOrElse(() -> Option.of(request.getParameter("username")).getOrElse(""));
        final String tenantId = Option.of(request.getAttribute(TENANT_ATTRIBUTE)).map(String::valueOf)
                .getOrElse(() -> Option.of(TenantContextHolder.getTenantId()).getOrElse(""));
        return new LoginAttemptDto(
                username,
                tenantId,
                Option.of(request.getRemoteAddr()).getOrElse(""),
                Option.of(request.getHeader("User-Agent")).getOrElse("")
        );
    }

    private LoginAttemptDto currentAttempt(String username) {
        return Option.of(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest)
                .map(request -> new LoginAttemptDto(
                        username,
                        Option.of(TenantContextHolder.getTenantId()).getOrElse(""),
                        Option.of(request.getRemoteAddr()).getOrElse(""),
                        Option.of(request.getHeader("User-Agent")).getOrElse("")
                ))
                .getOrElse(() -> new LoginAttemptDto(
                        username,
                        Option.of(TenantContextHolder.getTenantId()).getOrElse(""),
                        "",
                        ""
                ));
    }
}
