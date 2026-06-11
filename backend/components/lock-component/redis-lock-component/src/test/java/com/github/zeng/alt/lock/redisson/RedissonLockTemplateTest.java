package com.github.zeng.alt.lock.redisson;

import com.github.zeng.alt.lock.api.DistributedLock;
import com.github.zeng.alt.lock.model.LockInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 闂傚倸鍊风粈渚€骞夐敓鐘偓鍐幢濡炴洘妞藉浠嬵敇閻愭彃浜堕梻浣筋潐瀹曟绮旈鈧畷鐑筋敇濞戞ü澹曢梺鎸庣箓妤犳悂鐛Ο璁崇箚闁告瑥顦伴崐鎰版煛鐏炶濡奸柍钘夘槸閳诲酣骞嬮悙鏉戞櫛edissonLockTemplate
 *
 * @author zengJiaJun
 * @since 2026婵?6闂?1闂?
 * @version 1.0
 */
class RedissonLockTemplateTest {

    private RedissonClient redissonClient;
    private RLock rLock;
    private RedissonLockTemplate template;

    @BeforeEach
    void setUp() {
        redissonClient = mock(RedissonClient.class);
        rLock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        template = new RedissonLockTemplate(redissonClient);
    }

    @Test
    void testExecuteWithSupplier() {
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        doNothing().when(rLock).lock();
        doNothing().when(rLock).unlock();

        String result = template.execute("lock:supplier", () -> "done");
        assertEquals("done", result);
        verify(rLock).lock();
        verify(rLock).unlock();
    }

    @Test
    void testExecuteWithRunnable() {
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        doNothing().when(rLock).lock();
        doNothing().when(rLock).unlock();

        AtomicInteger counter = new AtomicInteger(0);
        template.execute("lock:runnable", counter::incrementAndGet);
        assertEquals(1, counter.get());
        verify(rLock).lock();
        verify(rLock).unlock();
    }

    @Test
    void testExecuteWithTimeoutAndSupplierSuccess() throws InterruptedException {
        when(rLock.tryLock(100, 200, TimeUnit.MILLISECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        doNothing().when(rLock).unlock();

        String result = template.execute("lock:timed:supplier", 100, 200, TimeUnit.MILLISECONDS, () -> "ok");
        assertEquals("ok", result);
        verify(rLock).tryLock(100, 200, TimeUnit.MILLISECONDS);
        verify(rLock).unlock();
    }

    @Test
    void testExecuteWithTimeoutAndSupplierFailure() throws InterruptedException {
        when(rLock.tryLock(100, 200, TimeUnit.MILLISECONDS)).thenReturn(false);

        String result = template.execute("lock:timed:fail", 100, 200, TimeUnit.MILLISECONDS, () -> "shouldNotReach");
        assertNull(result);
        verify(rLock, never()).unlock();
    }

    @Test
    void testExecuteWithTimeoutAndRunnableSuccess() throws InterruptedException {
        when(rLock.tryLock(100, 200, TimeUnit.MILLISECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        doNothing().when(rLock).unlock();

        AtomicInteger counter = new AtomicInteger(0);
        template.execute("lock:timed:runnable", 100, 200, TimeUnit.MILLISECONDS, counter::incrementAndGet);
        assertEquals(1, counter.get());
        verify(rLock).unlock();
    }

    @Test
    void testGetLock() {
        when(redissonClient.getLock("myLock")).thenReturn(rLock);

        DistributedLock lock = template.getLock("myLock");
        assertNotNull(lock);
        assertInstanceOf(RedissonDistributedLock.class, lock);
        verify(redissonClient).getLock("myLock");
    }

    @Test
    void testGetFairLock() {
        when(redissonClient.getFairLock("fairLock")).thenReturn(rLock);

        DistributedLock lock = template.getFairLock("fairLock");
        assertNotNull(lock);
        assertInstanceOf(RedissonDistributedLock.class, lock);
        verify(redissonClient).getFairLock("fairLock");
    }

    @Test
    void testTryLock() {
        when(rLock.tryLock()).thenReturn(true);

        assertTrue(template.tryLock("try:lock"));
        verify(rLock).tryLock();
    }

    @Test
    void testTryLockWithTimeout() throws InterruptedException {
        when(rLock.tryLock(100, TimeUnit.MILLISECONDS)).thenReturn(true);

        assertTrue(template.tryLock("try:timeout", 100, TimeUnit.MILLISECONDS));
        verify(rLock).tryLock(100, TimeUnit.MILLISECONDS);
    }

    @Test
    void testLock() {
        doNothing().when(rLock).lock();

        template.lock("direct:lock");
        verify(rLock).lock();
    }

    @Test
    void testUnlockWhenHeld() {
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        doNothing().when(rLock).unlock();

        template.unlock("held:lock");
        verify(rLock).isHeldByCurrentThread();
        verify(rLock).unlock();
    }

    @Test
    void testUnlockWhenNotHeld() {
        when(rLock.isHeldByCurrentThread()).thenReturn(false);

        template.unlock("not:held");
        verify(rLock, never()).unlock();
    }

    @Test
    void testIsLocked() {
        when(rLock.isLocked()).thenReturn(true);

        assertTrue(template.isLocked("locked:check"));
        verify(rLock).isLocked();
    }

    @Test
    void testLock4jApiLockWithAcquireTimeout() throws InterruptedException {
        when(rLock.tryLock(500, 1000, TimeUnit.MILLISECONDS)).thenReturn(true);

        LockInfo info = template.lock("lock4j:key", 1000, 500, null);
        assertNotNull(info);
        assertEquals("lock4j:key", info.getLockKey());
    }

    @Test
    void testLock4jApiLockWithoutExpire() throws InterruptedException {
        when(rLock.tryLock(500, TimeUnit.MILLISECONDS)).thenReturn(true);

        LockInfo info = template.lock("lock4j:noexpire", -1, 500, null);
        assertNotNull(info);
        verify(rLock).tryLock(500, TimeUnit.MILLISECONDS);
    }

    @Test
    void testLock4jApiLockBlocking() {
        doNothing().when(rLock).lock();

        LockInfo info = template.lock("lock4j:blocking", 1000, 0, null);
        assertNotNull(info);
        verify(rLock).lock();
    }

    @Test
    void testReleaseLock() {
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        doNothing().when(rLock).unlock();

        LockInfo info = new LockInfo("release:key", "val", 1000, 500, 1, rLock, null);
        assertTrue(template.releaseLock(info));
        verify(rLock).isHeldByCurrentThread();
        verify(rLock).unlock();
    }

    @Test
    void testReleaseLockNotHeld() {
        when(rLock.isHeldByCurrentThread()).thenReturn(false);

        LockInfo info = new LockInfo("release:notheld", "val", 1000, 500, 1, rLock, null);
        assertFalse(template.releaseLock(info));
        verify(rLock, never()).unlock();
    }

    @Test
    void testReleaseNullLockInfo() {
        assertFalse(template.releaseLock(null));
    }
}