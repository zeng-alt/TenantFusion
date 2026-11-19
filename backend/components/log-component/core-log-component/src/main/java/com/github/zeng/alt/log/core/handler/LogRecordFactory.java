package com.github.zeng.alt.log.core.handler;

import tools.jackson.core.JacksonException;
import com.github.zeng.alt.log.OperLogEvent;
import com.github.zeng.alt.log.core.operation.LogInvocation;

public interface LogRecordFactory {

    OperLogEvent create(LogInvocation invocation) throws JacksonException;

}