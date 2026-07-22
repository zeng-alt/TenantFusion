package com.github.zeng.alt.security.rbac.client.collector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Collection;
import java.util.List;

/**
 * Servlet 环境路由模板采集器。
 *
 * <p>从 Spring MVC 的 {@link RequestMappingHandlerMapping} 中提取所有的
 * {@code @RequestMapping} 路径模板（含 {@code @GetMapping}、{@code @PostMapping} 等组合注解）。</p>
 */
@Slf4j
public class ServletRouteTemplateCollector implements RouteTemplateCollector {

    private final Collection<RequestMappingHandlerMapping> handlerMappings;

    public ServletRouteTemplateCollector(Collection<RequestMappingHandlerMapping> handlerMappings) {
        this.handlerMappings = handlerMappings;
        log.debug("ServletRouteTemplateCollector initialized with {} handler mappings", handlerMappings.size());
    }

    @Override
    public List<String> collectTemplates() {
        List<String> templates = handlerMappings.stream()
                .flatMap(mapping -> mapping.getHandlerMethods().keySet().stream())
                .flatMap(info -> info.getPatternValues().stream())
                .distinct()
                .sorted()
                .toList();
        log.debug("Collected {} route templates from Servlet handler mappings", templates.size());
        return templates;
    }
}
