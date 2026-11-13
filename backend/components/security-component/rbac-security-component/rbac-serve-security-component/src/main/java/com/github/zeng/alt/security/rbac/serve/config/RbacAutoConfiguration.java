package com.github.zeng.alt.security.rbac.serve.config;


import com.github.zeng.alt.security.rbac.serve.router.RouteTemplateEventListener;
import com.github.zeng.alt.security.rbac.serve.RbacServeRuntimeHints;
import com.github.zeng.alt.security.rbac.serve.repository.DefaultRbacResourceService;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceLoader;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceService;
import com.github.zeng.alt.security.rbac.serve.router.RouteTemplateManager;
import com.github.zeng.alt.storage.StorageTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;


/**
 * RBAC 服务端核心自动配置。
 *
 * <p>无条件创建以下 Bean：</p>
 * <ul>
 *   <li>{@link RouteTemplateManager} — 路由模板管理器，负责路径模板匹配（如有消息队列则订阅远程事件）</li>
 * </ul>
 *
 * <p>条件创建以下 Bean：</p>
 * <ul>
 *   <li>{@link RbacResourceService} — 缓存优先的权限数据服务，依赖 {@link StorageTemplate} + {@link RbacResourceLoader}</li>
 * </ul>
 *
 * <p><strong>注意</strong>：{@code security.context.enabled-access=true} 时启动容器会强制要求存在 {@link RbacResourceLoader} Bean，
 * 用户必须提供实现，否则上下文刷新失败。</p>
 */
@AutoConfiguration
@ImportRuntimeHints(RbacServeRuntimeHints.class)
@Slf4j
@ConditionalOnBooleanProperty(value = "security.context.enabled-access", matchIfMissing = true)
public class RbacAutoConfiguration {

    @Bean
    public RouteTemplateManager routeTemplateManager() {
        return new RouteTemplateManager();
    }

    @Bean
    @ConditionalOnProperty(prefix = "alt.event.bindings.route-template", name = "transport")
    public RouteTemplateEventListener routeTemplateEventListener(RouteTemplateManager manager) {
        return new RouteTemplateEventListener(manager);
    }

    @Bean
    @ConditionalOnMissingBean(RbacResourceService.class)
    public RbacResourceService rbacResourceService(StorageTemplate storageTemplate, RbacResourceLoader resourceLoader) {
        log.debug("Creating DefaultRbacResourceService with StorageTemplate + RbacResourceLoader");
        return new DefaultRbacResourceService(storageTemplate, resourceLoader);
    }
}
