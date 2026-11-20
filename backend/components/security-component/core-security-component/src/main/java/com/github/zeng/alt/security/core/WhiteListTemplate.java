package com.github.zeng.alt.security.core;

import com.github.zeng.alt.security.api.WhiteListAccess;
import com.github.zeng.alt.security.api.WhiteListRule;
import com.github.zeng.alt.security.api.WhiteListService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 两类白名单的链式配置入口和共享请求分类器。
 * 每次请求读取规则源，仅在规则变化时重新编译；同一请求的基础授权与 ABAC 共用分类结果。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public class WhiteListTemplate {
    private static final String ACCESS_ATTRIBUTE = WhiteListTemplate.class.getName() + ".access";
    private final WhiteListService service;
    private final Set<WhiteListRule> permitAll = new LinkedHashSet<>();
    private final Set<WhiteListRule> authenticated = new LinkedHashSet<>();
    private volatile WhiteListSnapshot snapshot = new WhiteListSnapshot(Set.of(), Set.of(), List.of(), List.of());

    public WhiteListTemplate(WhiteListService service) {
        this.service = service;
    }

    public WhiteListTemplate permitAll(String... paths) {
        Arrays.stream(paths).map(WhiteListRule::new).forEach(permitAll::add);
        return this;
    }

    public WhiteListTemplate permitAll(HttpMethod method, String... paths) {
        Arrays.stream(paths).map(path -> new WhiteListRule(method.name(), path)).forEach(permitAll::add);
        return this;
    }

    public WhiteListTemplate authenticated(String... paths) {
        Arrays.stream(paths).map(WhiteListRule::new).forEach(authenticated::add);
        return this;
    }

    public WhiteListTemplate authenticated(HttpMethod method, String... paths) {
        Arrays.stream(paths).map(path -> new WhiteListRule(method.name(), path)).forEach(authenticated::add);
        return this;
    }

    /** 按应用内路径分类，避免 context path 影响规则匹配。 */
    public WhiteListAccess resolve(HttpServletRequest request) {
        final WhiteListMatch match = resolve(request.getMethod(),
                request.getRequestURI().substring(request.getContextPath().length()), request.getAttribute(ACCESS_ATTRIBUTE));
        request.setAttribute(ACCESS_ATTRIBUTE, match);
        return match.access();
    }

    /** Servlet 与 WebFlux 使用同一套方法、路径和规则优先级。 */
    public WhiteListAccess resolve(ServerWebExchange exchange) {
        final WhiteListMatch match = resolve(exchange.getRequest().getMethod().name(),
                exchange.getRequest().getPath().pathWithinApplication().value(), exchange.getAttribute(ACCESS_ATTRIBUTE));
        exchange.getAttributes().put(ACCESS_ATTRIBUTE, match);
        return match.access();
    }

    /** 规则源变化在下一个请求生效；两个组中完全相同的规则属于配置冲突。 */
    public WhiteListTemplate refresh() {
        final Set<WhiteListRule> publicRules = Stream.concat(permitAll.stream(), service.getPermitAll().stream())
                .collect(Collectors.toUnmodifiableSet());
        final Set<WhiteListRule> loginRules = Stream.concat(authenticated.stream(), service.getAuthenticated().stream())
                .collect(Collectors.toUnmodifiableSet());
        if (publicRules.equals(snapshot.publicRules()) && loginRules.equals(snapshot.loginRules())) {
            return this;
        }
        final Set<WhiteListRule> conflicts = publicRules.stream().filter(loginRules::contains)
                .collect(Collectors.toUnmodifiableSet());
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("公开白名单与登录白名单规则冲突: " + conflicts);
        }
        snapshot = new WhiteListSnapshot(publicRules, loginRules, compile(publicRules), compile(loginRules));
        return this;
    }

    private WhiteListAccess resolve(String method, PathContainer path) {
        refresh();
        final WhiteListSnapshot current = snapshot;
        if (current.publicMatchers().stream().anyMatch(matcher -> matcher.matches(method, path))) {
            return WhiteListAccess.PERMIT_ALL;
        }
        return current.loginMatchers().stream().anyMatch(matcher -> matcher.matches(method, path))
                ? WhiteListAccess.AUTHENTICATED : WhiteListAccess.PROTECTED;
    }

    private WhiteListMatch resolve(String method, String path, Object cached) {
        if (cached instanceof WhiteListMatch match && match.method().equals(method) && match.path().equals(path)) {
            return match;
        }
        return new WhiteListMatch(method, path, resolve(method, PathContainer.parsePath(path)));
    }

    private List<WhiteListMatcher> compile(Set<WhiteListRule> rules) {
        return rules.stream().map(rule -> new WhiteListMatcher(
                rule.method(), PathPatternParser.defaultInstance.parse(rule.path()))).toList();
    }

    private record WhiteListMatcher(
        String method,
        PathPattern path
    ) {
        boolean matches(String requestMethod, PathContainer requestPath) {
            return (method.isEmpty() || method.equals(requestMethod)) && path.matches(requestPath);
        }
    }

    private record WhiteListMatch(
        String method,
        String path,
        WhiteListAccess access
    ) {
    }

    private record WhiteListSnapshot(
        Set<WhiteListRule> publicRules,
        Set<WhiteListRule> loginRules,
        List<WhiteListMatcher> publicMatchers,
        List<WhiteListMatcher> loginMatchers
    ) {
    }
}
