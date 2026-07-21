package com.github.zeng.alt.security.rbac.serve.handler;


import com.github.zeng.alt.security.api.HttpResource;
import com.github.zeng.alt.security.api.Resource;
import com.github.zeng.alt.security.rbac.serve.manager.ResourceQueryManager;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.List;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年12月11日 21:49
 */
public class HttpResourceHandler extends AbstractResourceHandler {
    public HttpResourceHandler(ResourceQueryManager resourceQueryManager) {
        super(resourceQueryManager);
    }

    @Override
    public boolean matcher(HttpServletRequest request) {
        return true;
    }

    @Override
    public Boolean handler(Authentication authentication, RequestAuthorizationContext object) {
        List<Resource> resources = resourceQueryManager.query(new HttpResource(), authentication);
        for (Resource resource : resources) {
            if (resource.compareTo(object.getRequest())) {
                return true;
            }
        }
        return false;
    }

    public HttpResource create(RequestAuthorizationContext object) {
        HttpResource httpResource = new HttpResource();
        HttpServletRequest request = object.getRequest();
        httpResource.setUri(request.getRequestURI());
        httpResource.setMethod(request.getMethod());
        return httpResource;
    }
}
