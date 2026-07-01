package com.github.zeng.alt.log.core.interceptor;

import com.github.zeng.alt.log.core.handler.LogHandler;
import com.github.zeng.alt.log.core.operation.LogInvocation;
import com.github.zeng.alt.log.core.operation.LogOperation;
import com.github.zeng.alt.log.core.operation.LogOperationSource;
import lombok.RequiredArgsConstructor;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Pointcut;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.Ordered;

import java.lang.reflect.Method;
import java.time.Duration;

@RequiredArgsConstructor
public class LogMethodInterceptor implements MethodInterceptor, PointcutAdvisor, Ordered, AopInfrastructureBean {

    private final LogHandler handler;
    private final LogOperationSource source;
    private final Pointcut pointcut;
    private final Integer order;


    @Override
    public Object invoke(MethodInvocation invocation)
            throws Throwable {

        Method method =
                invocation.getMethod();

        Class<?> target =
                AopUtils.getTargetClass(
                        invocation.getThis());

        LogOperation operation =
                source.getLogOperation(method, target);

        if (operation == null) {
            return invocation.proceed();
        }

        long begin = System.nanoTime();

        Object result = null;

        Throwable throwable = null;

        try {

            result = invocation.proceed();

            return result;

        } catch (Throwable ex) {

            throwable = ex;

            throw ex;

        } finally {

            handler.handle(
                    LogInvocation.builder()
                            .invocation(invocation)
                            .operation(operation)
                            .result(result)
                            .throwable(throwable)
                            .duration(Duration.ofNanos(
                                    System.nanoTime() - begin))
                            .build());
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