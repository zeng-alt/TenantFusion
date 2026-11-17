package com.github.zeng.alt.log.message.consumer;

import com.github.zeng.alt.event.annotation.EventSubscription;
import com.github.zeng.alt.log.LoginInfoEvent;
import com.github.zeng.alt.log.message.producer.MessageLogProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 消费登录日志并交给本地持久化监听器，保存失败交由消息框架重试。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class LoginMessageLogConsumer {
    private final ApplicationEventPublisher eventPublisher;

    @EventSubscription(id = "login-log", binding = "log-login")
    public void onLoginLog(LoginInfoEvent event) {
        MessageLogProducer.setFromMessage(true);
        try {
            eventPublisher.publishEvent(event);
        } finally {
            MessageLogProducer.setFromMessage(false);
        }
    }
}
