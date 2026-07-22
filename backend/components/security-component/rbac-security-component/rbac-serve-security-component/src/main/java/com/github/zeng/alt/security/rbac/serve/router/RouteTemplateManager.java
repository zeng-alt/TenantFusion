package com.github.zeng.alt.security.rbac.serve.router;


import com.github.zeng.alt.message.Message;
import com.github.zeng.alt.message.MessageListener;
import com.github.zeng.alt.message.MessageQueueTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 路由模板管理器。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>维护 {@link RouteTemplateTrie} 前缀树，用于路径模板匹配</li>
 *   <li>通过 {@link MessageQueueTemplate} 订阅远程路由注册事件（微服务模式）</li>
 *   <li>提供 {@link #addRouteTemplate(String, List)} 方法供本地直接注册（单体模式）</li>
 * </ul>
 *
 * <p>无消息队列时，单体模式的 {@code DirectRouteTemplateRegistrar} 直接调用 {@code addRouteTemplate()} 注册路由。</p>
 */
@Slf4j
public class RouteTemplateManager implements MessageListener<RouteTemplateEvent>, InitializingBean {

    private static final String ROUTE_TEMPLATE_TOPIC = "route:template:path";

    private final RouteTemplateTrie trie = new RouteTemplateTrie();
    private MessageQueueTemplate messageQueueTemplate;

    public RouteTemplateManager() {
    }

    public RouteTemplateManager(MessageQueueTemplate messageQueueTemplate) {
        this.messageQueueTemplate = messageQueueTemplate;
    }

    /**
     * 注册或刷新指定上下文路径下的所有路由模板。
     * <p>先删除旧模板，再逐条插入新模板。</p>
     *
     * @param contextPath 上下文路径，用于删除旧路由
     * @param templates   路由模板列表
     */
    public void addRouteTemplate(String contextPath, List<String> templates) {
        log.debug("Adding {} route templates for contextPath '{}'", templates.size(), contextPath);
        trie.deleteSubtree(contextPath);
        for (String template : templates) {
            trie.insert(template);
        }
        log.info("Registered {} route templates for contextPath '{}'", templates.size(), contextPath);
    }

    /**
     * 将实际请求路径匹配到路由模板。
     *
     * @param path 实际请求路径
     * @return 匹配到的路由模板，无匹配时返回原路径
     */
    public String match(String path) {
        String match = trie.match(path);
        return StringUtils.hasText(match) ? match : path;
    }

    @Override
    public void onMessage(Message<RouteTemplateEvent> message) {
        RouteTemplateEvent event = message.getPayload();
        log.debug("Received route template event via message queue: contextPath='{}', templates={}",
                event.getContextPath(), event.getTemplates().size());
        this.addRouteTemplate(event.getContextPath(), event.getTemplates());
    }

    @Override
    public void afterPropertiesSet() {
        if (this.messageQueueTemplate != null) {
            log.debug("Subscribing to route template topic: {}", ROUTE_TEMPLATE_TOPIC);
            this.messageQueueTemplate.subscribe(ROUTE_TEMPLATE_TOPIC, this);
        } else {
            log.debug("No MessageQueueTemplate available, skipping topic subscription");
        }
    }
}
