package com.github.zeng.alt.security.rbac.client.collector;

import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import java.util.Collection;
import java.util.List;

public class ReactiveRouteTemplateCollector implements RouteTemplateCollector {

    private final Collection<RequestMappingHandlerMapping> handlerMappings;

    public ReactiveRouteTemplateCollector(Collection<RequestMappingHandlerMapping> handlerMappings) {
        this.handlerMappings = handlerMappings;
    }

    @Override
    public List<String> collectTemplates() {
        return handlerMappings.stream()
                .flatMap(mapping -> mapping.getHandlerMethods().keySet().stream())
                .flatMap(info -> info.getPatternsCondition().getPatterns().stream())
                .map(PathPattern::getPatternString)
                .distinct()
                .sorted()
                .toList();
    }
}
