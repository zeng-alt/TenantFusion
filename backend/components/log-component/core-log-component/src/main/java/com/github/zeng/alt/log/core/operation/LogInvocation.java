package com.github.zeng.alt.log.core.operation;

import lombok.Builder;
import lombok.Getter;
import org.aopalliance.intercept.MethodInvocation;

import java.time.Duration;

@Builder
@Getter
public class LogInvocation {

    private MethodInvocation invocation;

    private LogOperation operation;

    private Object result;

    private Throwable throwable;

    private Duration duration;

}