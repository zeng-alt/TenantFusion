package com.github.zeng.alt.log.message.producer;

import com.github.zeng.alt.event.EventTemplate;
import com.github.zeng.alt.log.LoginInfoEvent;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.event.EventListener;

/**
 * 将本地登录日志发往 log-login，跳过由消息消费回流的事件。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@CommonsLog
@RequiredArgsConstructor
public class LoginMessageLogProducer {
    private final EventTemplate messageQueue;

    @EventListener
    public void onLoginLog(LoginInfoEvent event) {
        if (MessageLogProducer.isFromMessage()) {
            return;
        }
        Try.run(() -> messageQueue.to("log-login").publish(event))
                .onFailure(error -> log.error("Failed to send LoginInfoEvent to message queue", error));
    }
}
