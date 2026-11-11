package com.github.zeng.alt.security.abac.method;

import com.github.zeng.alt.security.abac.AbacAuthorizationBuilder;
import com.github.zeng.alt.security.abac.AbacContext;
import com.github.zeng.alt.security.abac.AbacFailure;
import com.github.zeng.alt.security.abac.AbacMethodContext;
import com.github.zeng.alt.security.abac.AbacResourceResolver;
import com.github.zeng.alt.security.abac.AbacTemplate;

import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.security.access.AccessDeniedException;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 方法级 ABAC 前置拦截器：注册表命中注解 → 宿主主体解析（认证/租户/RBAC）→ 环境与注解声明的
 * 资源解析器（按类从容器取 bean）组装可信上下文 → AbacTemplate 属性约束。任何失败均 fail-closed
 * 抛 {@link AccessDeniedException}。方法参数按名及 #p0/#a0 自动注入 {@link AbacMethodContext}。
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
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

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
        final AbacMethodContext context = context(invocation, method, targetClass, attribute.get());
        final Either<String, Boolean> result = authorize(context, attribute.get());
        if (result.isLeft()) {
            throw new AccessDeniedException(result.getLeft());
        }
        if (!result.get()) {
            throw new AccessDeniedException("没有访问权限");
        }
        return invocation.proceed();
    }

    private Either<String, Boolean> authorize(AbacMethodContext context, AbacAuthorizeAttribute attribute) {
        return subjectProvider.resolve(attribute.permission())
                .flatMap(subject -> assembleContext(context, attribute, subject))
                .flatMap(abacContext -> {
                    final AbacAuthorizationBuilder builder = template.authorize(abacContext);
                    if (attribute.requirePolicy()) {
                        builder.requirePolicy();
                    }
                    return builder.get().mapLeft(AbacFailure::message);
                });
    }

    private Either<String, AbacContext> assembleContext(
            AbacMethodContext context, AbacAuthorizeAttribute attribute, AbacSubject subject) {
        return Try.of(() -> {
            final Map<String, Object> environment = Map.copyOf(environmentProvider.environment(context));
            final Map<String, Object> resource = new LinkedHashMap<>();
            for (Class<? extends AbacResourceResolver> type : attribute.resources()) {
                resource.putAll(beanFactory.getBean(type).resolve(context));
            }
            return new AbacContext(subject.tenant(), attribute.permission(),
                    subject.attributes(), resource, environment);
        }).toEither().mapLeft(ignored -> "授权属性无效");
    }

    private AbacMethodContext context(
            MethodInvocation invocation, Method method, Class<?> targetClass, AbacAuthorizeAttribute attribute) {
        final Object[] args = invocation.getArguments();
        final String[] names = parameterNameDiscoverer.getParameterNames(method);
        final Map<String, Object> paramByName = new LinkedHashMap<>();
        for (int index = 0; index < args.length; index++) {
            if (names != null && index < names.length) {
                paramByName.put(names[index], args[index]);
            }
            paramByName.put("p" + index, args[index]);
            paramByName.put("a" + index, args[index]);
        }
        return new AbacMethodContext(method, targetClass, args, paramByName, attribute.permission());
    }
}