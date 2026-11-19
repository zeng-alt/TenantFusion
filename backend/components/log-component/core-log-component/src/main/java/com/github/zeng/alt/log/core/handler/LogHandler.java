package com.github.zeng.alt.log.core.handler;

import tools.jackson.core.JacksonException;
import com.github.zeng.alt.log.core.operation.LogInvocation;

import java.lang.reflect.Method;

public interface LogHandler {

    void handle(LogInvocation invocation) throws JacksonException;

}