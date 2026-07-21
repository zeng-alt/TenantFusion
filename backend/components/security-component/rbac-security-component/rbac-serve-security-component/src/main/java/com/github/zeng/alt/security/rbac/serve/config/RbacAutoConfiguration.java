package com.github.zeng.alt.security.rbac.serve.config;


import com.github.zeng.alt.message.MessageQueueTemplate;
import com.github.zeng.alt.security.rbac.serve.RbacServeRuntimeHints;
import com.github.zeng.alt.security.rbac.serve.router.RouteTemplateManager;
import com.github.zeng.alt.security.rbac.serve.repository.DefaultRbacResourceService;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceService;
import com.github.zeng.alt.storage.StorageTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年11月26日 21:40
 */
@AutoConfiguration
@ImportRuntimeHints(RbacServeRuntimeHints.class)
public class RbacAutoConfiguration {

    @Bean
    public RouteTemplateManager routeTemplateManager(MessageQueueTemplate messageQueueTemplate) {
        return new RouteTemplateManager(messageQueueTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(RbacResourceService.class)
    public RbacResourceService rbacResourceService(StorageTemplate storageTemplate) {
        return new DefaultRbacResourceService(storageTemplate);
    }
}
