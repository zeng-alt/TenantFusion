package com.github.zeng.alt.lock;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MapAccessor;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringValueResolver;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 基于 SpEL 的方法表达式计算器，支持：
 * <ul>
 *   <li>#p0, #p1... / #a0, #a1... — 按参数索引引用</li>
 *   <li>#参数名 — 按参数名引用</li>
 *   <li>#root — 方法对象</li>
 *   <li>@beanName — 引用 Spring Bean</li>
 * </ul>
 *
 * @author zengJiaJun
 * @since 2026年06月09日
 * @version 1.0
 */
public class SpelMethodBasedExpressionEvaluator
        implements MethodBasedExpressionEvaluator, EmbeddedValueResolverAware, BeanFactoryAware {

    private static final MapAccessor MAP_ACCESSOR = new MapAccessor();

    private final Map<String, Expression> expressionCache = new ConcurrentReferenceHashMap<>(16);
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    private BeanResolver beanResolver;
    private StringValueResolver embeddedValueResolver;

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) {
        this.beanResolver = new BeanFactoryResolver(beanFactory);
    }

    @Override
    public void setEmbeddedValueResolver(@NonNull StringValueResolver resolver) {
        this.embeddedValueResolver = resolver;
    }

    @Override
    public <T> T getValue(Method method, Object[] arguments, String expression,
                          Class<T> resultType, @NonNull Map<String, Object> variables) {
        EvaluationContext context = createEvaluationContext(method, arguments);
        variables.forEach(context::setVariable);
        Expression exp = parseExpression(expression);
        return exp.getValue(context, resultType);
    }

    /**
     * 创建评估上下文
     */
    protected EvaluationContext createEvaluationContext(Method method, Object[] args) {
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                method, method, args, parameterNameDiscoverer);
        context.setBeanResolver(beanResolver);
        context.addPropertyAccessor(MAP_ACCESSOR);
        return context;
    }

    /**
     * 解析并缓存表达式
     */
    protected Expression parseExpression(String expression) {
        return expressionCache.computeIfAbsent(expression, exp -> {
            String resolved = embeddedValueResolver.resolveStringValue(exp);
            Assert.notNull(resolved, "Expression must not be null: " + exp);
            return expressionParser.parseExpression(resolved);
        });
    }
}
