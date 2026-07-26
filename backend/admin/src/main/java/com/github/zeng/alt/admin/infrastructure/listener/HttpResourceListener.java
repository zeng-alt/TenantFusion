package com.github.zeng.alt.admin.infrastructure.listener;

import com.github.zeng.alt.admin.infrastructure.entity.HttpResource;
import com.github.zeng.alt.bean.ApplicationContextHelper;
import com.github.zeng.alt.storage.StorageTemplate;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import jakarta.annotation.Resource;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;

import static com.github.zeng.alt.security.rbac.serve.repository.DefaultRbacResourceService.PERMISSION_KEY_PREFIX;

/**
 * @author zengJiaJun
 * @since 2026年07月26日
 * @version 1.0
 */
public class HttpResourceListener {

    @Resource
    private StorageTemplate storageTemplate;

    public HttpResourceListener() {
        ApplicationContextHelper.autowireBean(this);
    }

    @PostLoad
    public void postLoad(HttpResource httpResource) {
        httpResource.setOldPath(httpResource.getPath());
        httpResource.setOldMethod(httpResource.getMethod());
    }

    @PostUpdate
    public void postUpdate(HttpResource httpResource) {
        removeCache(httpResource);
    }

    @PostRemove
    public void postRemove(HttpResource httpResource) {
        removeCache(httpResource);
    }

    private void removeCache(HttpResource httpResource) {
        storageTemplate.opsForString().delete(PERMISSION_KEY_PREFIX + TenantContextHolder.getTenantId() + ":" + com.github.zeng.alt.security.api.HttpResource.of(httpResource.getOldPath(), httpResource.getOldMethod()).getKey());
    }
}
