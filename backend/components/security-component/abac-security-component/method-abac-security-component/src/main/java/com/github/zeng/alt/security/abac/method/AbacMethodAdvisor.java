package com.github.zeng.alt.security.abac.method;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;

/**
 * 方法授权 Advisor，order 默认取 Spring Security PRE_AUTHORIZE 同档（先于事务拦截器）。
 * 注册为 bean 后由 Spring 自动代理创建器拾取。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class AbacMethodAdvisor extends AbstractPointcutAdvisor {
    private final Advice advice;
    private final Pointcut pointcut = new AbacMethodPointcut();

    public AbacMethodAdvisor(AbacMethodInterceptor interceptor, int order) {
        this.advice = interceptor;
        setOrder(order);
    }

    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }

    @Override
    public Advice getAdvice() {
        return advice;
    }
}