package com.github.zeng.alt.security.rbac.serve.router;


import com.github.zeng.alt.message.Message;
import com.github.zeng.alt.message.MessageListener;
import com.github.zeng.alt.message.MessageQueueTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2025年04月07日 15:12
 */
@Slf4j
public class RouteTemplateManager implements MessageListener<RouteTemplateEvent>, InitializingBean {

    private static final String ROUTE_TEMPLATE_TOPIC = "route:template:path";

    private final RouteTemplateTrie trie = new RouteTemplateTrie();
    private final MessageQueueTemplate messageQueueTemplate;

    public RouteTemplateManager(MessageQueueTemplate messageQueueTemplate) {
        this.messageQueueTemplate = messageQueueTemplate;
    }

    public void addRouteTemplate(String contextPath, List<String> templates) {
        trie.deleteSubtree(contextPath);
        for (String template : templates) {
            trie.insert(template);
        }
    }

    public String match(String path) {
        String match = trie.match(path);
        return StringUtils.hasText(match) ? match : path;
    }

    @Override
    public void onMessage(Message<RouteTemplateEvent> message) {
        RouteTemplateEvent event = message.getPayload();
        this.addRouteTemplate(event.getContextPath(), event.getTemplates());
        log.info("{} 路由模板更新成功", event.getContextPath());
    }

    @Override
    public void afterPropertiesSet() {
        this.messageQueueTemplate.subscribe(ROUTE_TEMPLATE_TOPIC, this);
    }
}
