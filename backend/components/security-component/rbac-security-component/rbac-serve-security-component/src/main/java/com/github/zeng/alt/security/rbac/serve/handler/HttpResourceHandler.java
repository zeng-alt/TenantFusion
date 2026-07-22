package com.github.zeng.alt.security.rbac.serve.handler;

import com.github.zeng.alt.security.api.HttpResource;
import com.github.zeng.alt.security.api.Resource;
import com.github.zeng.alt.security.rbac.serve.manager.ResourceQueryManager;
import com.github.zeng.alt.security.rbac.serve.router.RouteTemplateManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.List;

/**
 * Servlet 环境默认 HTTP 资源处理器。
 *
 * <p>作为 {@link ParseManager} 的最终 fallback，始终匹配（{@code matcher()} 返回 {@code true}）。
 * 鉴权逻辑：从 {@link ResourceQueryManager} 获取当前用户可访问的 HTTP 资源列表，
 * 逐个调用 {@link Resource#compareTo(HttpServletRequest)} 匹配当前请求。</p>
 */
@Slf4j
public class HttpResourceHandler extends AbstractResourceHandler {

    private final RouteTemplateManager routeTemplateManager;

    public HttpResourceHandler(ResourceQueryManager resourceQueryManager, RouteTemplateManager routeTemplateManager) {
        super(resourceQueryManager);
        this.routeTemplateManager = routeTemplateManager;
    }

    @Override
    public boolean matcher(HttpServletRequest request) {
        return true;
    }

    @Override
    public Boolean handler(Authentication authentication, RequestAuthorizationContext object) {
        String uri = object.getRequest().getRequestURI();
        String method = object.getRequest().getMethod();
        String template = this.routeTemplateManager.match(uri);

        List<Resource> resources = resourceQueryManager.query(create(template, method), authentication);
        log.debug("Authorizing {} {} for user '{}', found {} permitted resources",
                method, uri, authentication.getName(), resources.size());
        for (Resource resource : resources) {
            if (resource.getMethod().equalsIgnoreCase(method) && resource.getUri().equalsIgnoreCase(template)) {
                log.debug("GRANT {} {} to user '{}'", method, uri, authentication.getName());
                return true;
            }
        }
        log.debug("DENY {} {} to user '{}'", method, uri, authentication.getName());
        return false;
    }

    private HttpResource create(String path, String method) {
        HttpResource httpResource = new HttpResource();
        httpResource.setUri(path);
        httpResource.setMethod(method);
        return httpResource;
    }
}
