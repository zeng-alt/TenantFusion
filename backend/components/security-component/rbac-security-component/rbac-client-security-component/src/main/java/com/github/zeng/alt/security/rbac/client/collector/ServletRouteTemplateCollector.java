package com.github.zeng.alt.security.rbac.client.collector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.function.*;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.*;
import java.util.function.Function;

@Slf4j
public class ServletRouteTemplateCollector implements RouteTemplateCollector {

    private final Collection<RequestMappingHandlerMapping> handlerMappings;
    private final List<RouterFunction<?>> routerFunctions;

    public ServletRouteTemplateCollector(
            Collection<RequestMappingHandlerMapping> handlerMappings,
            List<RouterFunction<?>> routerFunctions) {
        this.handlerMappings = handlerMappings;
        this.routerFunctions = routerFunctions;
        log.debug("ServletRouteTemplateCollector initialized with {} handler mappings and {} router functions",
                handlerMappings.size(), routerFunctions.size());
    }

    @Override
    public List<String> collectTemplates() {
        List<String> templates = new ArrayList<>();

        collectRequestMappingTemplates(templates);
        collectRouterFunctionTemplates(templates);

        List<String> result = templates.stream()
                .distinct()
                .sorted()
                .toList();
        log.debug("Collected {} route templates from Servlet environment", result.size());
        return result;
    }

    private void collectRequestMappingTemplates(List<String> templates) {
        handlerMappings.stream()
                .flatMap(mapping -> mapping.getHandlerMethods().keySet().stream())
                .forEach(info -> {
                    Set<String> patterns = info.getPatternValues();
                    Set<RequestMethod> methods = info.getMethodsCondition() != null
                            ? info.getMethodsCondition().getMethods()
                            : Set.of();
                    if (methods.isEmpty()) {
                        templates.addAll(patterns);
                    } else {
                        for (String pattern : patterns) {
                            for (RequestMethod method : methods) {
                                templates.add(method.name() + ":" + pattern);
                            }
                        }
                    }
                });
    }

    private void collectRouterFunctionTemplates(List<String> templates) {
        for (RouterFunction<?> rf : routerFunctions) {
            rf.accept(new RouterFunctions.Visitor() {
                private final Deque<String> prefixStack = new ArrayDeque<>();
                private String prefix = "";

                private String concatPaths(String base, String path) {
                    if (base.isEmpty()) return path;
                    if (path.isEmpty()) return base;
                    boolean baseEndsWithSlash = base.endsWith("/");
                    boolean pathStartsWithSlash = path.startsWith("/");
                    if (baseEndsWithSlash && pathStartsWithSlash) {
                        return base + path.substring(1);
                    } else if (!baseEndsWithSlash && !pathStartsWithSlash) {
                        return base + "/" + path;
                    } else {
                        return base + path;
                    }
                }

                @Override
                public void startNested(RequestPredicate predicate) {
                    PredicateInfo info = parsePredicate(predicate);
                    if (info.path != null) {
                        prefixStack.push(prefix);
                        prefix = concatPaths(prefix, info.path);
                    }
                }

                @Override
                public void endNested(RequestPredicate predicate) {
                    PredicateInfo info = parsePredicate(predicate);
                    if (info.path != null && !prefixStack.isEmpty()) {
                        prefix = prefixStack.pop();
                    }
                }

                @Override
                public void route(RequestPredicate predicate, HandlerFunction<?> handlerFunction) {
                    PredicateInfo info = parsePredicate(predicate);
                    if (info.path == null) return;
                    String fullPath = concatPaths(prefix, info.path);
                    if (info.methods.isEmpty()) {
                        templates.add(fullPath);
                    } else {
                        for (HttpMethod method : info.methods) {
                            templates.add(method.name() + ":" + fullPath);
                        }
                    }
                }

                @Override
                public void resources(Function<ServerRequest, Optional<Resource>> lookupFunction) {

                }

                @Override
                public void attributes(Map<String, Object> attributes) {

                }

                @Override
                public void unknown(RouterFunction<?> routerFunction) {

                }
            });
        }
    }

    private static PredicateInfo parsePredicate(RequestPredicate predicate) {
        PredicateInfo info = new PredicateInfo();
        Deque<PredicateInfo> stack = new ArrayDeque<>();
        stack.push(info);

        predicate.accept(new RequestPredicates.Visitor() {
            @Override
            public void path(String pattern) {
                stack.peek().path = pattern;
            }

            @Override
            public void method(Set<HttpMethod> methods) {
                stack.peek().methods.addAll(methods);
            }

            @Override
            public void startAnd() {
                stack.push(new PredicateInfo());
            }

            @Override
            public void and() {
                stack.push(new PredicateInfo());
            }

            @Override
            public void endAnd() {
                PredicateInfo right = stack.pop();
                PredicateInfo left = stack.pop();
                PredicateInfo merged = new PredicateInfo();
                merged.path = left.path != null ? left.path : right.path;
                merged.methods.addAll(left.methods);
                merged.methods.addAll(right.methods);
                stack.push(merged);
            }

            @Override
            public void startOr() {
                stack.push(new PredicateInfo());
            }

            @Override
            public void or() {
                stack.push(new PredicateInfo());
            }

            @Override
            public void endOr() {
                endAnd();
            }

            @Override
            public void startNegate() {}

            @Override
            public void endNegate() {}

            @Override
            public void header(String name, String value) {}

            @Override
            public void param(String name, String value) {}

            @Override
            public void pathExtension(String extension) {}

            @Override
            public void unknown(RequestPredicate p) {}
        });

        return stack.peek();
    }

    private static class PredicateInfo {
        String path;
        final Set<HttpMethod> methods = new HashSet<>();
    }
}
