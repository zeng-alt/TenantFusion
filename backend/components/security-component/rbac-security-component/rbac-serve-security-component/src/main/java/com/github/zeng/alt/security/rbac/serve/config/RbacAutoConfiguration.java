package com.github.zeng.alt.security.rbac.serve.config;


import com.github.zeng.alt.message.MessageQueueTemplate;
import com.github.zeng.alt.security.rbac.serve.RbacServeRuntimeHints;
import com.github.zeng.alt.security.rbac.serve.repository.DefaultRbacResourceService;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceLoader;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceService;
import com.github.zeng.alt.security.rbac.serve.router.RouteTemplateManager;
import com.github.zeng.alt.storage.StorageTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
@ConditionalOnBooleanProperty("security.context.enabled-access")
public class RbacAutoConfiguration {

    @Bean
    public RouteTemplateManager routeTemplateManager(ObjectProvider<MessageQueueTemplate> messageQueueTemplateProvider) {
        MessageQueueTemplate template = messageQueueTemplateProvider.getIfAvailable();
        if (template != null) {
            log.debug("Creating RouteTemplateManager with MessageQueueTemplate (microservices mode)");
            return new RouteTemplateManager(template);
        }
        log.debug("Creating RouteTemplateManager without MessageQueueTemplate (direct registration mode)");
        return new RouteTemplateManager();
    }

    @Bean
    @ConditionalOnMissingBean(RbacResourceService.class)
    public RbacResourceService rbacResourceService(StorageTemplate storageTemplate, RbacResourceLoader resourceLoader) {
        log.debug("Creating DefaultRbacResourceService with StorageTemplate + RbacResourceLoader");
        return new DefaultRbacResourceService(storageTemplate, resourceLoader);
    }
}
