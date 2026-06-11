package com.github.zeng.alt.lock;

import com.github.zeng.alt.lock.api.LockTemplate;
import com.github.zeng.alt.lock.api.NoOpLockTemplate;
import com.github.zeng.alt.lock.aop.LockAnnotationAdvisor;
import com.github.zeng.alt.lock.aop.LockInterceptor;
import com.github.zeng.alt.lock.model.DefaultLockFailureStrategy;
import com.github.zeng.alt.lock.model.DefaultLockKeyBuilder;
import com.github.zeng.alt.lock.model.LockFailureStrategy;
import com.github.zeng.alt.lock.model.LockKeyBuilder;
import com.github.zeng.alt.lock.config.LockProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 闆嗘垚娴嬭瘯锛歀ockAutoConfiguration 鑷姩閰嶇疆
 *
 * @author zengJiaJun
 * @since 2026骞?6鏈?1鏃?
 * @version 1.0
 */
@SpringBootTest(classes = LockAutoConfigurationTest.TestApplication.class, properties = {
        "spring.main.web-application-type=none"
})
class LockAutoConfigurationTest {

    @SpringBootApplication
    static class TestApplication {
    }

    @Autowired
    private ApplicationContext context;

    @Test
    void testMethodBasedExpressionEvaluatorBean() {
        assertNotNull(context.getBean(MethodBasedExpressionEvaluator.class));
        assertInstanceOf(SpelMethodBasedExpressionEvaluator.class,
                context.getBean(MethodBasedExpressionEvaluator.class));
    }

    @Test
    void testDefaultLockKeyBuilderBean() {
        LockKeyBuilder builder = context.getBean(LockKeyBuilder.class);
        assertNotNull(builder);
        assertInstanceOf(DefaultLockKeyBuilder.class, builder);
    }

    @Test
    void testDefaultLockFailureStrategyBean() {
        LockFailureStrategy strategy = context.getBean(LockFailureStrategy.class);
        assertNotNull(strategy);
        assertInstanceOf(DefaultLockFailureStrategy.class, strategy);
    }

    @Test
    void testNoOpLockTemplateBean() {
        LockTemplate template = context.getBean(LockTemplate.class);
        assertNotNull(template);
        assertInstanceOf(NoOpLockTemplate.class, template);
    }

    @Test
    void testLockInterceptorBean() {
        assertNotNull(context.getBean(LockInterceptor.class));
    }

    @Test
    void testLockAnnotationAdvisorBean() {
        assertNotNull(context.getBean(LockAnnotationAdvisor.class));
    }

    @Test
    void testLockPropertiesBean() {
        assertNotNull(context.getBean(LockProperties.class));
    }
}
