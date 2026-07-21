package com.github.zeng.alt.security.rbac.client.registrar;

import com.github.zeng.alt.message.MessageQueueTemplate;
import com.github.zeng.alt.security.rbac.client.RouteTemplateRegistrar;
import com.github.zeng.alt.security.rbac.client.collector.RouteTemplateCollector;
import com.github.zeng.alt.security.rbac.client.properties.RbacClientProperties;
import com.github.zeng.alt.security.rbac.serve.router.RouteTemplateEvent;

import java.util.List;

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
        messageQueueTemplate.send(ROUTE_TEMPLATE_TOPIC, event);
    }
}
