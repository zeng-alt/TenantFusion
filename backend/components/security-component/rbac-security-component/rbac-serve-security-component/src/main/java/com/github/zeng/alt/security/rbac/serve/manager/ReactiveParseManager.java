package com.github.zeng.alt.security.rbac.serve.manager;

import com.github.zeng.alt.security.rbac.serve.handler.ReactiveHttpResourceHandler;
import com.github.zeng.alt.security.rbac.serve.handler.ReactiveResourceHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Reactive 环境请求解析管理器。
 *
 * <p>遍历所有 {@link ReactiveResourceHandler}，通过 {@code matcher()} 方法查找匹配当前请求的处理器，
 * 无匹配时回退到 {@link ReactiveHttpResourceHandler}。</p>
 */
@Slf4j
public class ReactiveParseManager {

    private final List<ReactiveResourceHandler> reactiveResourceHandlers;
    private final ReactiveHttpResourceHandler reactiveHttpResourceHandler;

    public ReactiveParseManager(List<ReactiveResourceHandler> reactiveResourceHandlers, ReactiveHttpResourceHandler reactiveHttpResourceHandler) {
        this.reactiveResourceHandlers = reactiveResourceHandlers;
        this.reactiveHttpResourceHandler = reactiveHttpResourceHandler;
    }

    public Mono<ReactiveResourceHandler> parse(ServerWebExchange exchange) {
        return Flux.fromIterable(this.reactiveResourceHandlers)
                .flatMap(handler -> handler.matcher(exchange)
                        .filter(ServerWebExchangeMatcher.MatchResult::isMatch)
                        .map(result -> handler)
                )
                .next()
                .switchIfEmpty(Mono.just(reactiveHttpResourceHandler));
    }
}