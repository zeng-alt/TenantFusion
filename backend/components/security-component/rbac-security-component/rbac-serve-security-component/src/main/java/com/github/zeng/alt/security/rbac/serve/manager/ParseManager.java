package com.github.zeng.alt.security.rbac.serve.manager;

import com.github.zeng.alt.security.rbac.serve.handler.HttpResourceHandler;
import com.github.zeng.alt.security.rbac.serve.handler.ResourceHandler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Servlet 环境请求解析管理器。
 *
 * <p>遍历所有 {@link ResourceHandler}，通过 {@code matcher()} 方法查找匹配当前请求的处理器，
 * 无匹配时回退到 {@link HttpResourceHandler}。</p>
 */
@Slf4j
public class ParseManager {

    private final List<ResourceHandler> resourceHandlers;
    private final HttpResourceHandler httpResourceHandler;

    public ParseManager(List<ResourceHandler> resourceHandlers, HttpResourceHandler httpResourceHandler) {
        this.resourceHandlers = resourceHandlers;
        this.httpResourceHandler = httpResourceHandler;
        log.debug("ParseManager initialized with {} custom handlers", resourceHandlers.size());
    }

    /**
     * 解析请求，找到匹配的资源处理器。
     *
     * @param request HTTP 请求
     * @return 匹配的处理器，无匹配时返回默认的 {@link HttpResourceHandler}
     */
    public ResourceHandler parse(HttpServletRequest request) {
        return resourceHandlers
                .stream()
                .filter(handler -> handler.matcher(request))
                .findFirst()
                .orElse(httpResourceHandler);
    }
}
