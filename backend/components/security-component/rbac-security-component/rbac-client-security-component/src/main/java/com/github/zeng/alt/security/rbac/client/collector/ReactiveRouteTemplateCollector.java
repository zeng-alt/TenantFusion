package com.github.zeng.alt.security.rbac.client.collector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import java.util.Collection;
import java.util.List;

/**
 * Reactive 环境路由模板采集器。
 *
 * <p>从 WebFlux 的 {@link RequestMappingHandlerMapping} 中提取所有的
 * {@code @RequestMapping} 路径模板。</p>
 */
@Slf4j
public class ReactiveRouteTemplateCollector implements RouteTemplateCollector {

    private final Collection<RequestMappingHandlerMapping> handlerMappings;

    public ReactiveRouteTemplateCollector(Collection<RequestMappingHandlerMapping> handlerMappings) {
        this.handlerMappings = handlerMappings;
        log.debug("ReactiveRouteTemplateCollector initialized with {} handler mappings", handlerMappings.size());
    }

    @Override
    public List<String> collectTemplates() {
        List<String> templates = handlerMappings.stream()
                .flatMap(mapping -> mapping.getHandlerMethods().keySet().stream())
                .flatMap(info -> info.getPatternsCondition().getPatterns().stream())
                .map(PathPattern::getPatternString)
                .distinct()
                .sorted()
                .toList();
        log.debug("Collected {} route templates from WebFlux handler mappings", templates.size());
        return templates;
    }
}
