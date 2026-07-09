package com.github.zeng.alt.lock.model;

import com.github.zeng.alt.lock.exception.LockFailureException;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.core.log.LogMessage;

import java.lang.reflect.Method;

/**
 * 默认锁失败策略：抛出 {@link LockFailureException} 异常终止方法执行
 *
 * @author zengJiaJun
 * @since 2026年06月09日
 * @version 1.0
 */
@CommonsLog
public class DefaultLockFailureStrategy implements LockFailureStrategy {

    public static final String DEFAULT_MESSAGE = "request failed, please retry it.";

    @Override
    public void onLockFailure(String key, Method method, Object[] arguments) throws Throwable {
        log.warn(LogMessage.format("Lock acquisition failed for key [%s] on method [%s#%s]",
                key, method.getDeclaringClass().getSimpleName(), method.getName()));
        throw new LockFailureException(DEFAULT_MESSAGE);
    }
}
