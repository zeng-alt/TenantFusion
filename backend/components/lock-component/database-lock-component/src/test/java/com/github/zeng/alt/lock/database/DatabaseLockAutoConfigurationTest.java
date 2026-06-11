package com.github.zeng.alt.lock.database;

import com.github.zeng.alt.lock.api.LockTemplate;
import com.github.zeng.alt.lock.executor.LockExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: DatabaseLockAutoConfiguration
 *
 * @author zengJiaJun
 * @since 2026-06-11
 * @version 1.0
 */
@SpringBootTest(classes = DatabaseLockAutoConfigurationTest.TestApplication.class, properties = {
        "spring.main.web-application-type=none",
        "spring.datasource.url=jdbc:h2:mem:dblock_test;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:schema.sql'",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.autoconfigure.exclude=com.github.zeng.alt.lock.config.LockAutoConfiguration"
})
class DatabaseLockAutoConfigurationTest {

    @SpringBootApplication
    static class TestApplication {
    }

    @Autowired
    private ApplicationContext context;

    @Test
    void testDatabaseLockExecutorBean() {
        LockExecutor<?> executor = context.getBean(LockExecutor.class);
        assertNotNull(executor);
        assertInstanceOf(DatabaseLockExecutor.class, executor);
    }

    @Test
    void testDatabaseLockTemplateBean() {
        LockTemplate template = context.getBean(LockTemplate.class);
        assertNotNull(template);
        assertInstanceOf(DatabaseLockTemplate.class, template);
    }

    @Test
    void testLockAndRelease() {
        LockTemplate template = context.getBean(LockTemplate.class);
        String result = template.execute("test:auto:lock", () -> "db-locked");
        assertEquals("db-locked", result);
    }
}
