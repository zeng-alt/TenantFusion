package com.github.zeng.alt.security.rbac.client.collector;

import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Collection;
import java.util.List;

public class ServletRouteTemplateCollector implements RouteTemplateCollector {

    private final Collection<RequestMappingHandlerMapping> handlerMappings;

    public ServletRouteTemplateCollector(Collection<RequestMappingHandlerMapping> handlerMappings) {
        this.handlerMappings = handlerMappings;
    }

    @Override
    public List<String> collectTemplates() {
        return handlerMappings.stream()
                .flatMap(mapping -> mapping.getHandlerMethods().keySet().stream())
                .flatMap(info -> info.getPatternValues().stream())
                .distinct()
                .sorted()
                .toList();
    }
}
