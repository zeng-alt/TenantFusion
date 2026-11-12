package com.github.zeng.alt.security.abac.method;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.beans.factory.ObjectProvider;
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

    /** AOT 分析只需要切点，不提前创建依赖数据库的授权服务。 */
    AbacMethodAdvisor(ObjectProvider<AbacMethodInterceptor> interceptor, int order) {
        this.advice = (MethodInterceptor) invocation -> interceptor.getObject().invoke(invocation);
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