package com.github.zeng.alt.log.core.support;

import com.github.zeng.alt.log.core.operation.LogInvocation;

public interface RequestParameterResolver {

    String resolve(LogInvocation invocation);

}