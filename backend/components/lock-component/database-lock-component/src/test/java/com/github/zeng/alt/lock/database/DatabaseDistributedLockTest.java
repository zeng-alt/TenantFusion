package com.github.zeng.alt.lock.database;

import com.github.zeng.alt.lock.api.DistributedLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test: DatabaseDistributedLock
 *
 * @author zengJiaJun
 * @since 2026-06-11
 * @version 1.0
 */
class DatabaseDistributedLockTest {

    private DatabaseLockTemplate lockTemplate;
    private DatabaseDistributedLock lock;

    @BeforeEach
    void setUp() {
        EmbeddedDatabase db = new EmbeddedDatabaseBuilder().setName(UUID.randomUUID().toString())
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:schema.sql")
                .build();
        JdbcClient jdbcClient = JdbcClient.create(db);
        lockTemplate = new DatabaseLockTemplate(jdbcClient);
        lock = new DatabaseDistributedLock(lockTemplate, "dist:lock");
    }

    @Test
    void testName() {
        assertEquals("dist:lock", lock.name());
    }

    @Test
    void testTryLockImmediate() {
        assertTrue(lock.tryLock());
        assertTrue(lock.isHeldByCurrentThread());
        assertTrue(lock.isLocked());
        lock.unlock();
    }

    @Test
    void testTryLockDoubleFailure() {
        assertTrue(lock.tryLock());
        // H2 throws DuplicateKeyException on duplicate INSERT (PK constraint)
        assertThrows(DuplicateKeyException.class, () -> lock.tryLock());
    }

    @Test
    void testTryLockWithWaitTime() {
        assertTrue(lock.tryLock(100, TimeUnit.MILLISECONDS));
        lock.unlock();
    }

    @Test
    void testTryLockWithWaitAndLeaseTime() {
        assertTrue(lock.tryLock(100, 200, TimeUnit.MILLISECONDS));
        lock.unlock();
    }

    @Test
    void testLockBlocking() {
        lock.lock();
        assertTrue(lock.isHeldByCurrentThread());
        assertTrue(lock.isLocked());
        lock.unlock();
        assertFalse(lock.isLocked());
    }

    @Test
    void testIsHeldByCurrentThread() {
        assertFalse(lock.isHeldByCurrentThread());
        lock.tryLock();
        assertTrue(lock.isHeldByCurrentThread());
        lock.unlock();
        assertFalse(lock.isHeldByCurrentThread());
    }

    @Test
    void testIsLocked() {
        assertFalse(lock.isLocked());
        lock.tryLock();
        assertTrue(lock.isLocked());
        lock.unlock();
        assertFalse(lock.isLocked());
    }

    @Test
    void testCloseReleasesLock() {
        lock.lock();
        assertTrue(lock.isLocked());
        lock.close();
        assertFalse(lock.isLocked());
    }
}
