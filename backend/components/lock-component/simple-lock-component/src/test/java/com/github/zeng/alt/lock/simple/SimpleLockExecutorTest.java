package com.github.zeng.alt.lock.simple;

import org.junit.jupiter.api.Test;

import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 闂傚倷绀侀幉锟犮€冮崱妞曟椽寮介鐐插亶闂佽宕樼粔顕€宕烽娑樹壕闁挎繂楠搁獮妯讳繆閸欏鍊愰柡灞诲妼閳藉鈻庨幋鐐村晵impleLockExecutor
 *
 * @author zengJiaJun
 * @since 2026濠?6闂?1闂?
 * @version 1.0
 */
class SimpleLockExecutorTest {

    private final SimpleLockExecutor executor = new SimpleLockExecutor();

    @Test
    void testRenewalReturnsFalse() {
        assertFalse(executor.renewal());
    }

    @Test
    void testAcquireWithTimeoutPositive() {
        ReentrantLock lock = executor.acquire("key1", "val1", 1000, 500);
        assertNotNull(lock);
        assertTrue(lock.isHeldByCurrentThread());

        executor.releaseLock("key1", "val1", lock);
        assertFalse(lock.isHeldByCurrentThread());
    }

    @Test
    void testAcquireWithTimeoutNonPositive() {
        ReentrantLock lock = executor.acquire("key2", "val2", 1000, 0);
        assertNotNull(lock);
        assertTrue(lock.isHeldByCurrentThread());

        executor.releaseLock("key2", "val2", lock);
        assertFalse(lock.isHeldByCurrentThread());
    }

    @Test
    void testAcquireNegativeTimeout() {
        ReentrantLock lock = executor.acquire("key3", "val3", 1000, -1);
        assertNotNull(lock);
        assertTrue(lock.isHeldByCurrentThread());

        executor.releaseLock("key3", "val3", lock);
        assertFalse(lock.isHeldByCurrentThread());
    }

    @Test
    void testReleaseLockWhenNotHeld() {
        ReentrantLock lock = executor.acquire("key4", "val4", 1000, 100);
        assertNotNull(lock);

        // 闂傚倷绀佺紞濠傤焽瑜旈、鏍川椤旇棄寮块梺鍐叉惈閹冲繘鎮￠崒姘ｆ斀闁绘ɑ褰冮弳鐐烘煛婢跺﹦绉洪柡灞剧☉椤繈顢楅崟纰樺亾濡ゅ懏鐓曢柣鏃€褰冩禍楣冩⒑鐠囪尙绠扮紒缁樺灩閹广垽宕卞☉娆忓墾濡炪倖鐗滈崑鐐烘倿缂佹ɑ鍙忔俊顖濇閿涘秶绱?executor 闂傚倸鍊烽悞锕併亹閸愵喗鏅濋柕澶嗘櫅濡?
        lock.unlock();
        assertFalse(executor.releaseLock("key4", "val4", lock));
    }

    @Test
    void testAcquireWithContention() throws InterruptedException {
        ReentrantLock lock = executor.acquire("contention", "val", 1000, 1000);
        assertNotNull(lock);

        Thread t = new Thread(() -> {
            ReentrantLock result = executor.acquire("contention", "val2", 100, 200);
            assertNull(result);
        });
        t.start();
        t.join(1000);

        executor.releaseLock("contention", "val", lock);
    }
}
