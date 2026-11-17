package com.github.zeng.alt.log.jpa.event;

import com.github.zeng.alt.log.LoginInfoEvent;
import com.github.zeng.alt.log.jpa.service.LogPersistenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;

/**
 * 登录日志持久化监听器。
 * <p>
 * 监听 {@link LoginInfoEvent}，将日志保存到数据库。
 * 登录通常发生在无事务的过滤链中，保存使用独立事务。
 * 保存失败向上传播，由认证入口隔离，或交由 MQ 消费端重试。
 *
 * @author zengJiaJun
 * @since 2026-08-12
 * @version 1.0
 */
@RequiredArgsConstructor
public class LoginLogEventPersistenceListener {
    private final LogPersistenceService persistence;

    @EventListener
    public void handleLoginLog(LoginInfoEvent event) {
        persistence.create(event);
    }
}
