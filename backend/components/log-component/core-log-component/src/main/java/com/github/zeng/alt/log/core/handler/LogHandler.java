package com.github.zeng.alt.log.core.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.zeng.alt.log.core.operation.LogInvocation;

import java.lang.reflect.Method;

public interface LogHandler {

    void handle(LogInvocation invocation) throws JsonProcessingException;

}