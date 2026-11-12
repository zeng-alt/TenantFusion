package com.github.zeng.alt.security.core.web;


import com.github.zeng.alt.security.api.WhiteListService;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年11月27日 21:33
 */
public class WhiteListAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final WhiteListService whiteListService;
    private final AtomicReference<WhiteListSnapshot> snapshot = new AtomicReference<>(
            new WhiteListSnapshot(Set.of(), List.of()));

    public WhiteListAuthorizationManager(WhiteListService whiteListService) {
        this.whiteListService = whiteListService;
        refreshWhiteList();
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        refreshWhiteList();
        return new AuthorizationDecision(snapshot.get().matchers().stream()
                .anyMatch(matcher -> matcher.matches(context.getRequest())));
    }

    @Override
    public AuthorizationResult authorize(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        return check(authentication, context);
    }

    /** 白名单变更在下一次授权时生效，仅在快照变化时重新编译路径。 */
    public void refreshWhiteList() {
        final Set<String> paths = Set.copyOf(whiteListService.getWhiteList());
        if (paths.equals(snapshot.get().paths())) {
            return;
        }
        final List<PathPatternRequestMatcher> matchers = paths.stream()
                .map(path -> PathPatternRequestMatcher.withDefaults().matcher(path)).toList();
        snapshot.set(new WhiteListSnapshot(paths, matchers));
    }

    public static AuthorizationManager<RequestAuthorizationContext> authenticated(WhiteListService whiteListService) {
        return new WhiteListAuthorizationManager(whiteListService);
    }

    private record WhiteListSnapshot(Set<String> paths, List<PathPatternRequestMatcher> matchers) {
    }
}
