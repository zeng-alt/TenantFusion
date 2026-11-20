package com.github.zeng.alt.security.abac.method;

import com.github.zeng.alt.security.abac.AbacAuthorizationBuilder;
import com.github.zeng.alt.security.abac.AbacContext;
import com.github.zeng.alt.security.abac.AbacMethodContext;
import com.github.zeng.alt.security.abac.AbacInvocationDto;
import com.github.zeng.alt.security.abac.AbacResourceResolver;
import com.github.zeng.alt.security.abac.AbacTemplate;

import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 方法级 ABAC 拦截器：解析可信身份、资源和参数后执行前置规则，方法返回后在事务提交前执行后置规则。
 * 授权拒绝抛 {@link AccessDeniedException}；后置规则缺少活动事务时抛 {@link IllegalStateException}。
 * 方法参数按名及 #p0/#a0 自动注入 {@link AbacMethodContext}。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class AbacMethodInterceptor implements MethodInterceptor {
    private final AbacAuthorizeAttributeRegistry registry;
    private final AbacSubjectProvider subjectProvider;
    private final AbacEnvironmentProvider environmentProvider;
    private final ListableBeanFactory beanFactory;
    private final AbacTemplate template;
    private final AbacValueResolver values = new AbacValueResolver();

    public AbacMethodInterceptor(
            AbacAuthorizeAttributeRegistry registry,
            AbacSubjectProvider subjectProvider,
            AbacEnvironmentProvider environmentProvider,
            ListableBeanFactory beanFactory,
            AbacTemplate template
    ) {
        this.registry = registry;
        this.subjectProvider = subjectProvider;
        this.environmentProvider = environmentProvider;
        this.beanFactory = beanFactory;
        this.template = template;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        final Method method = invocation.getMethod();
        final Class<?> targetClass = invocation.getThis() != null
                ? invocation.getThis().getClass() : method.getDeclaringClass();
        final Option<AbacAuthorizeAttribute> attribute = registry.getAttribute(method, targetClass);
        if (attribute.isEmpty()) {
            return invocation.proceed();
        }
        final AbacMethodContext methodContext = context(invocation, attribute.get());
        final AbacContext context = subjectProvider.resolve(attribute.get().permission())
                .flatMap(subject -> assembleContext(methodContext, attribute.get(), subject))
                .getOrElseThrow(AccessDeniedException::new);
        require(context, attribute.get().requirePolicy());
        final Object returned = invocation.proceed();
        if (returned instanceof Either<?, ?> either && either.isLeft()
                || returned instanceof Option<?> option && option.isEmpty()) {
            return returned;
        }
        require(context.withInvocation(context.invocation().after(values.resolve(returned),
                TransactionSynchronizationManager.isActualTransactionActive())), false);
        return returned;
    }

    private void require(AbacContext context, boolean required) {
        final AbacAuthorizationBuilder builder = template.authorize(context);
        if (required) { builder.requirePolicy(); }
        final boolean granted = builder.get().getOrElseThrow(failure ->
                "POST_AUTH_REQUIRES_TRANSACTION".equals(failure.code())
                        ? new IllegalStateException(failure.message()) : new AccessDeniedException(failure.message()));
        if (!granted) { throw new AccessDeniedException("没有访问权限"); }
    }

    private Either<String, AbacContext> assembleContext(
            AbacMethodContext context, AbacAuthorizeAttribute attribute, AbacSubject subject) {
        return Try.of(() -> {
            final Map<String, Object> environment = Map.copyOf(environmentProvider.environment(context));
            final Map<String, Object> resource = new LinkedHashMap<>();
            for (Class<? extends AbacResourceResolver> type : attribute.resources()) {
                resource.putAll(beanFactory.getBean(type).resolve(context));
            }
            if (resource.containsKey("tenant") && !subject.tenant().equals(resource.get("tenant"))) {
                throw new AccessDeniedException("资源不属于当前租户");
            }
            return new AbacContext(subject.tenant(), attribute.permission(),
                    subject.attributes(), resource, environment).withInvocation(AbacInvocationDto.method(
                            subject.authentication(), variables(context), TransactionSynchronizationManager.isActualTransactionActive()));
        }).toEither().mapLeft(ignored -> "授权属性无效");
    }

    private Map<String, Object> variables(AbacMethodContext context) {
        final Map<String, Object> variables = new LinkedHashMap<>();
        final Map<Object, Object> projected = new java.util.IdentityHashMap<>();
        context.paramByName().forEach((name, value) -> variables.put(name,
                projected.computeIfAbsent(value, valuesKey -> values.resolve(valuesKey).getOrNull())));
        return variables;
    }

    private AbacMethodContext context(MethodInvocation invocation, AbacAuthorizeAttribute attribute) {
        final Object[] args = invocation.getArguments();
        final Map<String, Object> paramByName = new LinkedHashMap<>();
        final AbacMethodDto metadata = attribute.metadata();
        metadata.argumentIndices().forEach((name, index) -> paramByName.put(name, args[index]));
        return new AbacMethodContext(metadata.method(), metadata.targetClass(), args, paramByName, attribute.permission());
    }
}
