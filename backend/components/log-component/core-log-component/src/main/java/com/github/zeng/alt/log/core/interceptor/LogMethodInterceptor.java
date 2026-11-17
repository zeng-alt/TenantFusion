package com.github.zeng.alt.log.core.interceptor;

import com.github.zeng.alt.log.core.handler.LogHandler;
import com.github.zeng.alt.log.core.operation.LogInvocation;
import com.github.zeng.alt.log.core.operation.LogOperation;
import com.github.zeng.alt.log.core.operation.LogOperationSource;
import io.vavr.control.Try;
import java.lang.reflect.Method;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Pointcut;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;

@CommonsLog
@RequiredArgsConstructor
public class LogMethodInterceptor implements MethodInterceptor, PointcutAdvisor, Ordered, AopInfrastructureBean {

    private final ObjectProvider<LogHandler> handler;
    private final ObjectProvider<LogOperationSource> source;
    private final Pointcut pointcut;
    private final Integer order;


    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        final Method method = invocation.getMethod();
        final Class<?> target = AopUtils.getTargetClass(invocation.getThis());
        final LogOperation operation = Try.of(() -> source.getObject().getLogOperation(method, target))
                .onFailure(error -> log.warn("Failed to resolve operation log metadata", error)).getOrNull();
        if (operation == null) {
            return invocation.proceed();
        }
        final long begin = System.nanoTime();
        Object result = null;
        Throwable failure = null;
        try {
            result = invocation.proceed();
            return result;
        } catch (Throwable error) {
            failure = error;
            throw error;
        } finally {
            final LogInvocation snapshot = LogInvocation.builder()
                    .invocation(invocation).operation(operation).result(result).throwable(failure)
                    .duration(Duration.ofNanos(System.nanoTime() - begin)).build();
            Try.run(() -> handler.getObject().handle(snapshot))
                    .onFailure(error -> log.warn("Failed to record operation log", error));
        }
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }

    @Override
    public Advice getAdvice() {
        return this;
    }
}
