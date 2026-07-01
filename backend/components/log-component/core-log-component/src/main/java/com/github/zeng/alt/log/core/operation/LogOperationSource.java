package com.github.zeng.alt.log.core.operation;

import org.springframework.lang.Nullable;
import java.lang.reflect.Method;

public interface LogOperationSource {

    /**
     * 获取日志操作信息
     *
     * @param method 方法
     * @param targetClass 目标类
     * @return LogOperation
     */
    @Nullable
    LogOperation getLogOperation(Method method, @Nullable Class<?> targetClass);

}