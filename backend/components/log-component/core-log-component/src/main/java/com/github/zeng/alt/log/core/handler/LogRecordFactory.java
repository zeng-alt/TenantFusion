package com.github.zeng.alt.log.core.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.zeng.alt.log.OperLogEvent;
import com.github.zeng.alt.log.core.operation.LogInvocation;

public interface LogRecordFactory {

    OperLogEvent create(LogInvocation invocation) throws JsonProcessingException;

}