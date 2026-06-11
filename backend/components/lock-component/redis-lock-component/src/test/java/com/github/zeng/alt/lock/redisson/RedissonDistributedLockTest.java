package com.github.zeng.alt.lock.redisson;

import com.github.zeng.alt.lock.api.DistributedLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 闂傚倷绀侀幉锟犮€冮崱妞曟椽寮介鐐插亶闂佽宕樼粔顕€宕烽娑樹壕闁挎繂楠搁獮妯讳繆閸欏鍊愰柡灞诲妼閳藉鈻庨幋鐐村晱edissonDistributedLock
 *
 * @author zengJiaJun
 * @since 2026濠?6闂?1闂?
 * @version 1.0
 */
class RedissonDistributedLockTest {

    private RLock rLock;
    private DistributedLock distributedLock;

    @BeforeEach
    void setUp() {
        rLock = mock(RLock.class);
        when(rLock.getName()).thenReturn("test:lock");
        distributedLock = new RedissonDistributedLock(rLock);
    }

    @Test
    void testName() {
        assertEquals("test:lock", distributedLock.name());
        verify(rLock).getName();
    }

    @Test
    void testTryLock() {
        when(rLock.tryLock()).thenReturn(true);

        assertTrue(distributedLock.tryLock());
        verify(rLock).tryLock();
    }

    @Test
    void testTryLockWithWaitTime() throws InterruptedException {
        when(rLock.tryLock(100, TimeUnit.MILLISECONDS)).thenReturn(true);

        assertTrue(distributedLock.tryLock(100, TimeUnit.MILLISECONDS));
        verify(rLock).tryLock(100, TimeUnit.MILLISECONDS);
    }

    @Test
    void testTryLockWithWaitAndLeaseTime() throws InterruptedException {
        when(rLock.tryLock(100, 200, TimeUnit.MILLISECONDS)).thenReturn(true);

        assertTrue(distributedLock.tryLock(100, 200, TimeUnit.MILLISECONDS));
        verify(rLock).tryLock(100, 200, TimeUnit.MILLISECONDS);
    }

    @Test
    void testLock() {
        doNothing().when(rLock).lock();

        distributedLock.lock();
        verify(rLock).lock();
    }

    @Test
    void testUnlockWhenHeld() {
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        doNothing().when(rLock).unlock();

        distributedLock.unlock();
        verify(rLock).isHeldByCurrentThread();
        verify(rLock).unlock();
    }

    @Test
    void testUnlockWhenNotHeld() {
        when(rLock.isHeldByCurrentThread()).thenReturn(false);

        distributedLock.unlock();
        verify(rLock, never()).unlock();
    }

    @Test
    void testIsHeldByCurrentThread() {
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        assertTrue(distributedLock.isHeldByCurrentThread());
        verify(rLock).isHeldByCurrentThread();
    }

    @Test
    void testIsLocked() {
        when(rLock.isLocked()).thenReturn(true);

        assertTrue(distributedLock.isLocked());
        verify(rLock).isLocked();
    }

    @Test
    void testClose() {
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        doNothing().when(rLock).unlock();

        distributedLock.close();
        verify(rLock).unlock();
    }
}
