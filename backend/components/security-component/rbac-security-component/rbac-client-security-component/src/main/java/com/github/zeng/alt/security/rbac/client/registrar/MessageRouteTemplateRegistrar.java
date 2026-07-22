package com.github.zeng.alt.security.rbac.client.registrar;

import com.github.zeng.alt.message.MessageQueueTemplate;
import com.github.zeng.alt.security.rbac.client.RouteTemplateRegistrar;
import com.github.zeng.alt.security.rbac.client.collector.RouteTemplateCollector;
import com.github.zeng.alt.security.rbac.client.properties.RbacClientProperties;
import com.github.zeng.alt.security.rbac.serve.router.RouteTemplateEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 微服务模式路由注册器。
 *
 * <p>通过 {@link MessageQueueTemplate} 将路由模板事件发送到消息队列，
 * 由 Serve 端的 {@link com.github.zeng.alt.security.rbac.serve.router.RouteTemplateManager} 接收并处理。
 * 适用于 Client 和 Serve 在不同 JVM 中的部署场景。</p>
 */
@Slf4j
public class MessageRouteTemplateRegistrar extends RouteTemplateRegistrar {

    private static final String ROUTE_TEMPLATE_TOPIC = "route:template:path";

    private final MessageQueueTemplate messageQueueTemplate;

    public MessageRouteTemplateRegistrar(RouteTemplateCollector collector,
                                         RbacClientProperties properties,
                                         MessageQueueTemplate messageQueueTemplate) {
        super(collector, properties);
        this.messageQueueTemplate = messageQueueTemplate;
    }

    @Override
    protected void doRegister(String contextPath, List<String> templates) {
        RouteTemplateEvent event = new RouteTemplateEvent();
        event.setContextPath(contextPath);
        event.setTemplates(templates);
        log.info("Sending {} route templates via message queue topic '{}'", templates.size(), ROUTE_TEMPLATE_TOPIC);
        messageQueueTemplate.send(ROUTE_TEMPLATE_TOPIC, event);
    }
}
