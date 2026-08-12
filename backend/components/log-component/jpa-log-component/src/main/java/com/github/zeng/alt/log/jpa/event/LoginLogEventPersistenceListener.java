package com.github.zeng.alt.log.jpa.event;

import com.github.zeng.alt.log.LoginInfoEvent;
import com.github.zeng.alt.log.jpa.entity.LoginLogEntity;
import com.github.zeng.alt.log.jpa.repository.LoginLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.core.log.LogMessage;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

/**
 * 登录日志持久化监听器。
 * <p>
 * 监听 {@link LoginInfoEvent}，将日志保存到数据库。
 * 登录发生在 Spring Security 过滤链中，通常没有事务，
 * 因此使用 {@code fallbackExecution = true} 保证事件始终被处理。
 *
 * @author zengJiaJun
 * @since 2026-08-12
 * @version 1.0
 */
@CommonsLog
@RequiredArgsConstructor
public class LoginLogEventPersistenceListener {

    private final LoginLogRepository repository;

    @TransactionalEventListener(fallbackExecution = true)
    public void handleLoginLog(LoginInfoEvent event) {
        try {
            LoginLogEntity entity = convert(event);
            repository.save(entity);
            log.debug(LogMessage.format("Persisted login log: username=%s, status=%s", event.getUsername(), event.getStatus()));
        } catch (Exception e) {
            log.error(LogMessage.format("Failed to persist login log: username=%s", event.getUsername()), e);
        }
    }

    private LoginLogEntity convert(LoginInfoEvent event) {
        LoginLogEntity entity = new LoginLogEntity();
        entity.setTenantId(event.getTenantId());
        entity.setUsername(event.getUsername());
        entity.setIp(event.getIp());
        entity.setStatus(event.getStatus());
        entity.setMessage(event.getMessage());
        entity.setLoginTime(LocalDateTime.now());
        return entity;
    }
}
