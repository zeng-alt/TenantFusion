package com.github.zeng.alt.lock.redisson;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 闂傚倷绀侀幉锟犮€冮崱妞曟椽寮介鐐插亶闂佽宕樼粔顕€宕烽娑樹壕闁挎繂楠搁獮妯讳繆閸欏鍊愰柡灞诲妼閳藉鈻庨幋鐐村晱edissonLockExecutor
 *
 * @author zengJiaJun
 * @since 2026濠?6闂?1闂?
 * @version 1.0
 */
class RedissonLockExecutorTest {

    private RedissonClient redissonClient;
    private RLock rLock;
    private RedissonLockExecutor executor;

    @BeforeEach
    void setUp() {
        redissonClient = mock(RedissonClient.class);
        rLock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        executor = new RedissonLockExecutor(redissonClient);
    }

    @Test
    void testRenewalReturnsTrue() {
        assertTrue(executor.renewal());
    }

    @Test
    void testAcquireWithTimeoutAndLeaseTime() throws InterruptedException {
        when(rLock.tryLock(500, 1000, TimeUnit.MILLISECONDS)).thenReturn(true);

        RLock result = executor.acquire("key1", "val1", 1000, 500);
        assertSame(rLock, result);
        verify(rLock).tryLock(500, 1000, TimeUnit.MILLISECONDS);
    }

    @Test
    void testAcquireWithTimeoutOnly() throws InterruptedException {
        when(rLock.tryLock(500, TimeUnit.MILLISECONDS)).thenReturn(true);

        RLock result = executor.acquire("key2", "val2", -1, 500);
        assertSame(rLock, result);
        verify(rLock).tryLock(500, TimeUnit.MILLISECONDS);
    }

    @Test
    void testAcquireWithZeroExpire() throws InterruptedException {
        // expire <= 0 婵犵數鍋犻幓顏嗙礊閳ь剚绻涙径瀣鐎殿噮鍋婃俊鑸靛緞鐎ｎ亜澹嬮梺璇插嚱缂嶅棝宕板Δ鍛櫖鐎光偓閸曨剛鍘藉┑鐐叉閸旓箑鈻撻弴銏″殙闁冲搫鍟犻崑?
        when(rLock.tryLock(500, TimeUnit.MILLISECONDS)).thenReturn(true);

        RLock result = executor.acquire("key3", "val3", 0, 500);
        assertSame(rLock, result);
        verify(rLock).tryLock(500, TimeUnit.MILLISECONDS);
    }

    @Test
    void testAcquireBlocking() {
        doNothing().when(rLock).lock();

        RLock result = executor.acquire("key4", "val4", 1000, -1);
        assertSame(rLock, result);
        verify(rLock).lock();
    }

    @Test
    void testAcquireZeroTimeout() {
        doNothing().when(rLock).lock();

        RLock result = executor.acquire("key5", "val5", 1000, 0);
        assertSame(rLock, result);
        verify(rLock).lock();
    }

    @Test
    void testAcquireFailure() throws InterruptedException {
        when(rLock.tryLock(100, 200, TimeUnit.MILLISECONDS)).thenReturn(false);

        RLock result = executor.acquire("key6", "val6", 200, 100);
        assertNull(result);
    }

    @Test
    void testReleaseLockWhenHeld() {
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        doNothing().when(rLock).unlock();

        boolean released = executor.releaseLock("key7", "val7", rLock);
        assertTrue(released);
        verify(rLock).unlock();
    }

    @Test
    void testReleaseLockWhenNotHeld() {
        when(rLock.isHeldByCurrentThread()).thenReturn(false);

        boolean released = executor.releaseLock("key8", "val8", rLock);
        assertFalse(released);
        verify(rLock, never()).unlock();
    }
}
