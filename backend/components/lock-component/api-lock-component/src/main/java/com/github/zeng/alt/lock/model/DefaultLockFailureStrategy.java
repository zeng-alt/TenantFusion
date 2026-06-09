package com.github.zeng.alt.lock.model;

import com.github.zeng.alt.lock.exception.LockFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * 默认锁失败策略：抛出 {@link LockFailureException} 异常终止方法执行
 *
 * @author zengJiaJun
 * @since 2026年06月09日
 * @version 1.0
 */
public class DefaultLockFailureStrategy implements LockFailureStrategy {

    private static final Logger log = LoggerFactory.getLogger(DefaultLockFailureStrategy.class);

    public static final String DEFAULT_MESSAGE = "request failed, please retry it.";

    @Override
    public void onLockFailure(String key, Method method, Object[] arguments) throws Throwable {
        log.warn("Lock acquisition failed for key [{}] on method [{}#{}]",
                key, method.getDeclaringClass().getSimpleName(), method.getName());
        throw new LockFailureException(DEFAULT_MESSAGE);
    }
}
