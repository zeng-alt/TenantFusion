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
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年11月29日 21:15
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