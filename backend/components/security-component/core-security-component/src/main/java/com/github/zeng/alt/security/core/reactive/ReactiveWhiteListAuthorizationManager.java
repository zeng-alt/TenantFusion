package com.github.zeng.alt.security.core.reactive;


import com.github.zeng.alt.security.api.WhiteListService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年11月27日 21:33
 */
@RequiredArgsConstructor
public class ReactiveWhiteListAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private final WhiteListService whiteListService;

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext object) {
        Set<String> whiteList = whiteListService.getWhiteList();
        if (CollectionUtils.isEmpty(whiteList)) {
            return Mono.just(new AuthorizationDecision(false));
        }
        PathPatternParser parser = PathPatternParser.defaultInstance;

        List<PathPattern> pathPatterns = new ArrayList<>(whiteList.size());
        for (String pattern : whiteList) {
            pattern = parser.initFullPathPattern(pattern);
            PathPattern pathPattern = parser.parse(pattern);
            pathPatterns.add(pathPattern);
        }
        ServerWebExchangeMatcher serverWebExchangeMatcher = ServerWebExchangeMatchers.pathMatchers(pathPatterns.toArray(new PathPattern[0]));
        return serverWebExchangeMatcher.matches(object.getExchange()).map(result -> new AuthorizationDecision(result.isMatch()));
    }

    public static ReactiveAuthorizationManager<AuthorizationContext> authenticated(WhiteListService whiteListService) {
        return new ReactiveWhiteListAuthorizationManager(whiteListService);
    }
}
