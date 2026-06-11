package com.github.zeng.alt.lock.redisson;

import com.github.zeng.alt.lock.api.LockTemplate;
import com.github.zeng.alt.lock.executor.LockExecutor;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: RedissonLockAutoConfiguration
 *
 * @author zengJiaJun
 * @since 2026-06-11
 * @version 1.0
 */
@SpringBootTest(classes = RedissonLockAutoConfigurationTest.TestApplication.class, properties = {
        "spring.main.web-application-type=none",
        "spring.autoconfigure.exclude=com.github.zeng.alt.lock.config.LockAutoConfiguration"
})
class RedissonLockAutoConfigurationTest {

    @SpringBootApplication
    @Import(RedissonLockAutoConfiguration.class)
    static class TestApplication {
        @Bean
        public RedissonClient redissonClient() {
            return org.mockito.Mockito.mock(RedissonClient.class);
        }
    }

    @Autowired
    private ApplicationContext context;

    @Test
    void testRedissonLockExecutorBean() {
        LockExecutor<?> executor = context.getBean(LockExecutor.class);
        assertNotNull(executor);
        assertInstanceOf(RedissonLockExecutor.class, executor);
    }

    @Test
    void testRedissonLockTemplateBean() {
        LockTemplate template = context.getBean(LockTemplate.class);
        assertNotNull(template);
        assertInstanceOf(RedissonLockTemplate.class, template);
    }
}
