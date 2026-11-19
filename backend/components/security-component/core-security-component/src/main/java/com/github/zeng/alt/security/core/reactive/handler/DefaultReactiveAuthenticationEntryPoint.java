package com.github.zeng.alt.security.core.reactive.handler;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年11月27日 21:44
 */
@RequiredArgsConstructor
public class DefaultReactiveAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    DefaultReactiveAuthenticationEntryPoint() {
        this(JsonMapper.builder().build());
    }

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException exception) {
        final var response = exchange.getResponse();
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
        problem.setTitle("用户未登录或者Token无效/过期");
        problem.setInstance(exchange.getRequest().getURI());
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return Mono.fromCallable(() -> objectMapper.writeValueAsBytes(problem))
                .flatMap(bytes -> response.writeWith(Mono.just(response.bufferFactory().wrap(bytes))));
    }

    public static ServerAuthenticationEntryPoint handler() {
        return new DefaultReactiveAuthenticationEntryPoint();
    }

    /** 使用宿主配置的 JSON 序列化器创建默认处理器。 */
    public static ServerAuthenticationEntryPoint handler(ObjectMapper objectMapper) {
        return new DefaultReactiveAuthenticationEntryPoint(objectMapper);
    }
}
