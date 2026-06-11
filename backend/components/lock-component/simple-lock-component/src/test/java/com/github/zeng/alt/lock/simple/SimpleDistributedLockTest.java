package com.github.zeng.alt.lock.simple;

import com.github.zeng.alt.lock.api.DistributedLock;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 闂傚倷绀侀幉锟犮€冮崱妞曟椽寮介鐐插亶闂佽宕樼粔顕€宕烽娑樹壕闁挎繂楠搁獮妯讳繆閸欏鍊愰柡灞诲妼閳藉鈻庨幋鐐村晵impleDistributedLock
 *
 * @author zengJiaJun
 * @since 2026濠?6闂?1闂?
 * @version 1.0
 */
class SimpleDistributedLockTest {

    @Test
    void testName() {
        DistributedLock lock = new SimpleDistributedLock("test-lock");
        assertEquals("test-lock", lock.name());
    }

    @Test
    void testTryLockImmediate() {
        DistributedLock lock = new SimpleDistributedLock("immediate");
        assertTrue(lock.tryLock());
        assertTrue(lock.isHeldByCurrentThread());
        assertTrue(lock.isLocked());
        lock.unlock();
    }

    @Test
    void testTryLockWithWaitTime() {
        DistributedLock lock = new SimpleDistributedLock("wait-lock");
        assertTrue(lock.tryLock(100, TimeUnit.MILLISECONDS));
        assertTrue(lock.isLocked());
        lock.unlock();
        assertFalse(lock.isLocked());
    }

    @Test
    void testTryLockWithWaitAndLeaseTime() {
        DistributedLock lock = new SimpleDistributedLock("lease-lock");
        // Simple 闂備浇顕ф绋匡耿闁秴纾婚柣鏃囧亹瀹撲線鏌涢妷顔荤敖闁汇倐鍋撻梻浣风串缁蹭粙寮甸鈧埢?leaseTime闂傚倷鐒︾€笛呯矙閹达附鍎斿┑鍌氭啞閸嬨倝鏌熸潏鍓х暠閻熸瑱绠撻幃姗€鎮欑捄杞版睏闂?tryLock(waitTime, unit)
        assertTrue(lock.tryLock(100, 200, TimeUnit.MILLISECONDS));
        assertTrue(lock.isLocked());
        lock.unlock();
    }

    @Test
    void testLockBlocking() {
        DistributedLock lock = new SimpleDistributedLock("blocking-lock");
        lock.lock();
        assertTrue(lock.isHeldByCurrentThread());
        assertTrue(lock.isLocked());
        lock.unlock();
        assertFalse(lock.isLocked());
    }

    @Test
    void testUnlockWhenNotHeld() {
        DistributedLock lock = new SimpleDistributedLock("safe-unlock");
        assertDoesNotThrow(lock::unlock);
    }

    @Test
    void testIsHeldByCurrentThread() {
        DistributedLock lock = new SimpleDistributedLock("held-check");
        assertFalse(lock.isHeldByCurrentThread());
        lock.lock();
        assertTrue(lock.isHeldByCurrentThread());
        lock.unlock();
        assertFalse(lock.isHeldByCurrentThread());
    }

    @Test
    void testIsLocked() {
        DistributedLock lock = new SimpleDistributedLock("locked-check");
        assertFalse(lock.isLocked());
        lock.lock();
        assertTrue(lock.isLocked());
        lock.unlock();
        assertFalse(lock.isLocked());
    }

    @Test
    void testCloseReleasesLock() {
        DistributedLock lock = new SimpleDistributedLock("close-lock");
        lock.lock();
        assertTrue(lock.isLocked());
        lock.close();
        assertFalse(lock.isLocked());
    }

}
