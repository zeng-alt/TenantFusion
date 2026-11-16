package com.github.zeng.alt.excel.web.reactive;

import com.github.zeng.alt.excel.context.ExcelContextHandler;
import com.github.zeng.alt.excel.context.ExcelContextTemplate;
import com.github.zeng.alt.excel.context.SecurityExcelContextHandler;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.util.ClassUtils;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 将 WebFlux 原生安全上下文叠加到调用入口捕获的租户、语言与自定义上下文上。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
final class ExcelReactiveContextHelper {

    private ExcelReactiveContextHelper() {
    }

    static Mono<ExcelContextHandler.Snapshot> capture(ExcelContextHandler.Snapshot snapshot, boolean enabled) {
        if (enabled && ClassUtils.isPresent(
                "org.springframework.security.core.context.ReactiveSecurityContextHolder",
                ExcelReactiveContextHelper.class.getClassLoader()
        )) {
            return SecurityContextHelper.capture(snapshot);
        }
        return Mono.just(snapshot);
    }

    /**
     * 隔离可选 Spring Security 类型的链接。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    private static final class SecurityContextHelper {

        private static Mono<ExcelContextHandler.Snapshot> capture(ExcelContextHandler.Snapshot snapshot) {
            return ReactiveSecurityContextHolder.getContext()
                    .map(context -> ExcelContextTemplate.combine(List.of(
                            snapshot, new SecurityExcelContextHandler().capture(context)
                    )))
                    .defaultIfEmpty(snapshot);
        }
    }
}
