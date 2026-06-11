package com.github.zeng.alt.lock.aop;

import com.github.zeng.alt.lock.annotation.Lock;
import org.aopalliance.aop.Advice;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.StaticMethodMatcher;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.cglib.proxy.Proxy;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
/**
 * {@link Lock} 注解的 AOP 通知器
 *
 * @author zengJiaJun
 * @since 2026年06月09日
 * @version 1.0
 */

public class LockAnnotationAdvisor extends AbstractPointcutAdvisor implements BeanFactoryAware {

    private final Advice advice;
    private final Pointcut pointcut;

    public LockAnnotationAdvisor(LockInterceptor interceptor, int order) {
        this.advice = interceptor;
        this.pointcut = new ComposablePointcut(new AnnotationMethodPoint(Lock.class))
                .union(new ComposablePointcut(new AnnotationMethodPoint(Lock.List.class)));
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

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (this.advice instanceof BeanFactoryAware) {
            ((BeanFactoryAware) this.advice).setBeanFactory(beanFactory);
        }
    }

    private static class AnnotationMethodPoint implements Pointcut {

        private final Class<? extends Annotation> annotationType;

        AnnotationMethodPoint(Class<? extends Annotation> annotationType) {
            Assert.notNull(annotationType, "Annotation type must not be null");
            this.annotationType = annotationType;
        }

        @Override
        public ClassFilter getClassFilter() {
            return ClassFilter.TRUE;
        }

        @Override
        public MethodMatcher getMethodMatcher() {
            return new AnnotationMethodMatcher(annotationType);
        }

        private static class AnnotationMethodMatcher extends StaticMethodMatcher {
            private final Class<? extends Annotation> annotationType;

            AnnotationMethodMatcher(Class<? extends Annotation> annotationType) {
                this.annotationType = annotationType;
            }

            @Override
            public boolean matches(Method method, Class<?> targetClass) {

                if (matchesMethod(method)) {
                    return true;
                }

                if (targetClass == null) {
                    return false;
                }

                try {
                    Method specificMethod =
                            AopUtils.getMostSpecificMethod(method, targetClass);

                    return specificMethod != method
                            && matchesMethod(specificMethod);
                } catch (Throwable ex) {
                    return false;
                }
            }

            private boolean matchesMethod(Method method) {
                return AnnotatedElementUtils.hasAnnotation(method, this.annotationType);
            }
        }
    }

}
