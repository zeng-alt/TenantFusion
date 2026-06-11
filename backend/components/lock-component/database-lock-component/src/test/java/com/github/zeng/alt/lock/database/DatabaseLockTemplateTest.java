package com.github.zeng.alt.lock.database;

import com.github.zeng.alt.lock.api.DistributedLock;
import com.github.zeng.alt.lock.model.LockInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test: DatabaseLockTemplate (H2 in-memory)
 *
 * @author zengJiaJun
 * @since 2026-06-11
 * @version 1.0
 */
class DatabaseLockTemplateTest {

    private DatabaseLockTemplate template;
    private JdbcClient jdbcClient;

    @BeforeEach
    void setUp() {
        EmbeddedDatabase db = new EmbeddedDatabaseBuilder().setName(UUID.randomUUID().toString())
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:schema.sql")
                .build();
        jdbcClient = JdbcClient.create(db);
        template = new DatabaseLockTemplate(jdbcClient);
    }

    @Test
    void testExecuteWithSupplier() {
        String result = template.execute("execute:supplier", () -> "done");
        assertEquals("done", result);
    }

    @Test
    void testExecuteWithRunnable() {
        AtomicInteger counter = new AtomicInteger(0);
        template.execute("execute:runnable", counter::incrementAndGet);
        assertEquals(1, counter.get());
    }

    @Test
    void testExecuteWithTimeoutAndSupplierSuccess() {
        String result = template.execute("execute:timed:supplier", 100, 200, TimeUnit.MILLISECONDS, () -> "ok");
        assertEquals("ok", result);
    }

    @Test
    void testExecuteWithTimeoutAndRunnableSuccess() {
        AtomicInteger counter = new AtomicInteger(0);
        template.execute("execute:timed:runnable", 100, 200, TimeUnit.MILLISECONDS, counter::incrementAndGet);
        assertEquals(1, counter.get());
    }

    @Test
    void testGetLock() {
        DistributedLock lock = template.getLock("get:lock");
        assertNotNull(lock);
        assertEquals("get:lock", lock.name());
        assertInstanceOf(DatabaseDistributedLock.class, lock);
    }

    @Test
    void testGetFairLockSameAsGetLock() {
        DistributedLock lock = template.getFairLock("fair:lock");
        assertInstanceOf(DatabaseDistributedLock.class, lock);
    }

    @Test
    void testTryLock() {
        assertTrue(template.tryLock("try:lock"));
        // H2 throws DuplicateKeyException on duplicate INSERT (PK constraint)
        assertThrows(DuplicateKeyException.class, () -> template.tryLock("try:lock"));
    }

    @Test
    void testTryLockWithTimeout() {
        assertTrue(template.tryLock("try:timeout", 100, TimeUnit.MILLISECONDS));
        template.unlock("try:timeout");
    }

    @Test
    void testLock() {
        template.lock("lock:direct");
        assertTrue(template.isLocked("lock:direct"));
        template.unlock("lock:direct");
        assertFalse(template.isLocked("lock:direct"));
    }

    @Test
    void testIsLocked() {
        assertFalse(template.isLocked("is:locked:unknown"));
        template.lock("is:locked:check");
        assertTrue(template.isLocked("is:locked:check"));
        template.unlock("is:locked:check");
        assertFalse(template.isLocked("is:locked:check"));
    }

    @Test
    void testLock4jApiLock() {
        LockInfo info = template.lock("lock4j:key", 1000, 500, null);
        assertNotNull(info);
        assertEquals("lock4j:key", info.getLockKey());
        assertEquals(1000, info.getExpire());
        assertEquals(500, info.getAcquireTimeout());
        assertEquals(1, info.getAcquireCount());
        assertNotNull(info.getLockValue());

        assertTrue(template.releaseLock(info));
    }

    @Test
    void testLock4jApiReleaseNullLockInfo() {
        assertFalse(template.releaseLock((LockInfo) null));
    }

    @Test
    void testDoubleLockSameKeyReturnsFalse() {
        assertTrue(template.tryLock("double:lock"));
        // H2 throws DuplicateKeyException on duplicate INSERT (PK constraint)
        assertThrows(DuplicateKeyException.class, () -> template.tryLock("double:lock"));
    }

    @Test
    void testLockExpirationPreempt() throws InterruptedException {
        // Acquire lock with short expiration
        LockInfo info = template.lock("expire:key", 100, 100, null);
        assertNotNull(info);

        // Wait for lock to expire
        Thread.sleep(200);

        // After expiration, acquireLock tries INSERT first which still throws
        // because the row still exists (expired but not deleted)
        assertThrows(DuplicateKeyException.class, () -> template.tryLock("expire:key"));
    }
}
