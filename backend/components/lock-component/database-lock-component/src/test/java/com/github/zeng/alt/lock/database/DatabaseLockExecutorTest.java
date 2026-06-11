package com.github.zeng.alt.lock.database;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test: DatabaseLockExecutor (H2 in-memory)
 *
 * @author zengJiaJun
 * @since 2026-06-11
 * @version 1.0
 */
class DatabaseLockExecutorTest {

    private DatabaseLockExecutor executor;
    private JdbcClient jdbcClient;

    @BeforeEach
    void setUp() {
        EmbeddedDatabase db = new EmbeddedDatabaseBuilder().setName(UUID.randomUUID().toString())
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:schema.sql")
                .build();
        jdbcClient = JdbcClient.create(db);
        executor = new DatabaseLockExecutor(jdbcClient);
    }

    @Test
    void testAcquireNewLock() {
        String instance = executor.acquire("executor:new", "val1", 30000, 1000);
        assertNotNull(instance);
        assertEquals(32, instance.length());

        boolean released = executor.releaseLock("executor:new", "val1", instance);
        assertTrue(released);
    }

    @Test
    void testAcquireDuplicateLockReturnsNull() {
        String instance1 = executor.acquire("executor:dup", "val1", 30000, 1000);
        assertNotNull(instance1);

        // H2 throws DuplicateKeyException on duplicate INSERT (PK constraint)
        assertThrows(DuplicateKeyException.class,
                () -> executor.acquire("executor:dup", "val2", 30000, 100));

        executor.releaseLock("executor:dup", "val1", instance1);
    }

    @Test
    void testAcquirePreemptExpiredLock() throws InterruptedException {
        // Acquire lock with short expiration
        String instance1 = executor.acquire("executor:preempt", "val1", 50, 1000);
        assertNotNull(instance1);

        Thread.sleep(100);

        // Row still exists (expired but not deleted), INSERT throws
        assertThrows(DuplicateKeyException.class,
                () -> executor.acquire("executor:preempt", "val2", 30000, 1000));

        executor.releaseLock("executor:preempt", "val1", instance1);
    }

    @Test
    void testReleaseLockWrongInstance() {
        String instance = executor.acquire("executor:wrong", "val1", 30000, 1000);
        assertNotNull(instance);

        // Use wrong instanceId to release
        boolean released = executor.releaseLock("executor:wrong", "val1", "wrong-instance");
        assertFalse(released);

        executor.releaseLock("executor:wrong", "val1", instance);
    }

    @Test
    void testReleaseNonExistentLock() {
        boolean released = executor.releaseLock("executor:nonexistent", "val", "instance");
        assertFalse(released);
    }
}
