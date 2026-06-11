package com.github.zeng.alt.lock.simple;

import com.github.zeng.alt.lock.api.LockTemplate;
import com.github.zeng.alt.lock.executor.LockExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: SimpleLockAutoConfiguration
 *
 * @author zengJiaJun
 * @since 2026-06-11
 * @version 1.0
 */
@SpringBootTest(classes = SimpleLockAutoConfigurationTest.TestApplication.class, properties = {
        "spring.main.web-application-type=none",
        "spring.autoconfigure.exclude=com.github.zeng.alt.lock.config.LockAutoConfiguration"
})
class SimpleLockAutoConfigurationTest {

    @SpringBootApplication
    static class TestApplication {
    }

    @Autowired
    private ApplicationContext context;

    @Test
    void testSimpleLockExecutorBean() {
        LockExecutor<?> executor = context.getBean(LockExecutor.class);
        assertNotNull(executor);
        assertInstanceOf(SimpleLockExecutor.class, executor);
    }

    @Test
    void testSimpleLockTemplateBean() {
        LockTemplate template = context.getBean(LockTemplate.class);
        assertNotNull(template);
        assertInstanceOf(SimpleLockTemplate.class, template);
    }
}
