package com.github.zeng.alt.security.core.reactive.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年11月27日 21:44
 */
@RequiredArgsConstructor
public class DefaultReactiveAccessDeniedHandler implements ServerAccessDeniedHandler {
    private final ObjectMapper objectMapper;

    public DefaultReactiveAccessDeniedHandler() {
        this(Jackson2ObjectMapperBuilder.json().build());
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException exception) {
        final var response = exchange.getResponse();
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
        problem.setTitle("用户没有访问权限");
        problem.setInstance(exchange.getRequest().getURI());
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return Mono.fromCallable(() -> objectMapper.writeValueAsBytes(problem))
                .flatMap(bytes -> response.writeWith(Mono.just(response.bufferFactory().wrap(bytes))));
    }

    public static ServerAccessDeniedHandler handler() {
        return new DefaultReactiveAccessDeniedHandler();
    }

    /** 使用宿主配置的 JSON 序列化器创建默认处理器。 */
    public static ServerAccessDeniedHandler handler(ObjectMapper objectMapper) {
        return new DefaultReactiveAccessDeniedHandler(objectMapper);
    }
}
