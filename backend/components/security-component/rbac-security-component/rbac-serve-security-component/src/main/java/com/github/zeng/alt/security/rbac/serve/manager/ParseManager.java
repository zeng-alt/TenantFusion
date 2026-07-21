package com.github.zeng.alt.security.rbac.serve.manager;

import com.github.zeng.alt.security.rbac.serve.handler.HttpResourceHandler;
import com.github.zeng.alt.security.rbac.serve.handler.ResourceHandler;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年11月29日 21:14
 */
public class ParseManager {

    private final List<ResourceHandler> resourceHandlers;
    private final HttpResourceHandler httpResourceHandler;

    public ParseManager(List<ResourceHandler> resourceHandlers, HttpResourceHandler httpResourceHandler) {
        this.resourceHandlers = resourceHandlers;
        this.httpResourceHandler = httpResourceHandler;
    }

    public ResourceHandler parse(HttpServletRequest request) {
        return resourceHandlers
                .stream()
                .filter(handler -> handler.matcher(request))
                .findFirst()
                .orElse(httpResourceHandler);
    }
}
