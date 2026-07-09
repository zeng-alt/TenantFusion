package com.github.zeng.alt.log.jpa.event;

import com.github.zeng.alt.log.OperLogEvent;
import com.github.zeng.alt.log.jpa.entity.LogEntity;
import com.github.zeng.alt.log.jpa.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.core.log.LogMessage;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 操作日志持久化监听器。
 * <p>
 * 监听 {@link OperLogEvent}，将日志保存到数据库。
 * 默认在事务提交前执行（{@link TransactionalEventListener#phase()} = BEFORE_COMMIT）。
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
@CommonsLog
@RequiredArgsConstructor
public class LogEventPersistenceListener {

    private final LogRepository repository;

    /**
     * 处理操作日志事件，保存到数据库。
     */
    @TransactionalEventListener(phase = org.springframework.transaction.event.TransactionPhase.BEFORE_COMMIT)
    public void handleOperLog(OperLogEvent event) {
        try {
            LogEntity entity = convert(event);
            repository.save(entity);
            log.debug(LogMessage.format("Persisted oper log: title=%s, costTime=%sms", event.getTitle(), event.getCostTime()));
        } catch (Exception e) {
            log.error(LogMessage.format("Failed to persist oper log: title=%s", event.getTitle()), e);
        }
    }

    private LogEntity convert(OperLogEvent event) {
        LogEntity entity = new LogEntity();

        entity.setTenantId(event.getTenantId());
        entity.setTitle(event.getTitle());
        entity.setBusinessType(event.getBusinessType());
        entity.setMethod(event.getMethod());
        entity.setRequestMethod(event.getRequestMethod());
        entity.setOperatorType(event.getOperatorType());
        entity.setOperName(event.getOperName());
        entity.setDeptName(event.getDeptName());
        entity.setOperUrl(event.getOperUrl());
        entity.setOperIp(event.getOperIp());
        entity.setOperLocation(event.getOperLocation());
        entity.setOperParam(event.getOperParam());
        entity.setJsonResult(event.getJsonResult());
        entity.setStatus(event.getStatus());
        entity.setErrorMsg(event.getErrorMsg());
        entity.setOperTime(event.getOperTime());
        entity.setCostTime(event.getCostTime());

        return entity;
    }
}
