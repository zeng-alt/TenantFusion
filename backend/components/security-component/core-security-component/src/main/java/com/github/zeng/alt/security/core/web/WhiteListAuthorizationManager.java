package com.github.zeng.alt.security.core.web;


import com.github.zeng.alt.security.api.WhiteListService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年11月27日 21:33
 */
@RequiredArgsConstructor
public class WhiteListAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final WhiteListService whiteListService;

    private final AtomicReference<List<PathPatternRequestMatcher>> whiteMatchers =
            new AtomicReference<>(List.of());

    @Override
    public AuthorizationDecision check(
            Supplier<Authentication> authentication,
            RequestAuthorizationContext object) {

        HttpServletRequest request = object.getRequest();

        List<PathPatternRequestMatcher> matchers = whiteMatchers.get();
        for (PathPatternRequestMatcher matcher : matchers) {
            if (matcher.matches(request)) {
                return new AuthorizationDecision(true);
            }
        }

        return new AuthorizationDecision(false);
    }

    @Nullable
    @Override
    public AuthorizationResult authorize(Supplier<Authentication> authentication, RequestAuthorizationContext object) {
        HttpServletRequest request = object.getRequest();

        List<PathPatternRequestMatcher> matchers = whiteMatchers.get();
        for (PathPatternRequestMatcher matcher : matchers) {
            if (matcher.matches(request)) {
                return new AuthorizationDecision(true);
            }
        }

        return new AuthorizationDecision(false);
    }

    @Scheduled(fixedDelay = 60_000) // 每60秒刷新一次
    public void refreshWhiteList() {

        Set<String> whiteList = whiteListService.getWhiteList();

        this.whiteMatchers.set(whiteList.stream()
                .map(path -> PathPatternRequestMatcher.withDefaults().matcher(path))
                .toList());
    }


    public static AuthorizationManager<RequestAuthorizationContext> authenticated(WhiteListService whiteListService) {
        return new WhiteListAuthorizationManager(whiteListService);
    }
}
