package com.github.zeng.alt.lock.simple;

import com.github.zeng.alt.lock.api.DistributedLock;
import com.github.zeng.alt.lock.model.LockInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 闂傚倷绀侀幉锟犮€冮崱妞曟椽寮介鐐插亶闂佽宕樼粔顕€宕烽娑樹壕闁挎繂楠搁獮妯讳繆閸欏鍊愰柡灞诲妼閳藉鈻庨幋鐐村晵impleLockTemplate
 *
 * @author zengJiaJun
 * @since 2026濠?6闂?1闂?
 * @version 1.0
 */
class SimpleLockTemplateTest {

    private SimpleLockTemplate template;

    @BeforeEach
    void setUp() {
        template = new SimpleLockTemplate();
    }

    @Test
    void testExecuteWithSupplier() {
        String result = template.execute("lock1", () -> "done");
        assertEquals("done", result);
    }

    @Test
    void testExecuteWithRunnable() {
        AtomicInteger counter = new AtomicInteger(0);
        template.execute("lock2", counter::incrementAndGet);
        assertEquals(1, counter.get());
    }

    @Test
    void testExecuteWithTimeoutAndSupplierSuccess() {
        String result = template.execute("lock3", 100, 200, TimeUnit.MILLISECONDS, () -> "ok");
        assertEquals("ok", result);
    }

    @Test
    void testExecuteWithTimeoutAndRunnableSuccess() {
        AtomicInteger counter = new AtomicInteger(0);
        template.execute("lock4", 100, 200, TimeUnit.MILLISECONDS, counter::incrementAndGet);
        assertEquals(1, counter.get());
    }

    @Test
    void testGetLock() {
        DistributedLock lock = template.getLock("myLock");
        assertNotNull(lock);
        assertEquals("myLock", lock.name());
        assertInstanceOf(SimpleDistributedLock.class, lock);
    }

    @Test
    void testGetFairLock() {
        DistributedLock lock = template.getFairLock("fairLock");
        assertNotNull(lock);
        assertEquals("fairLock", lock.name());
        assertInstanceOf(SimpleDistributedLock.class, lock);
    }

    @Test
    void testTryLock() {
        assertTrue(template.tryLock("key1"));
        assertTrue(template.tryLock("key2"));
        template.unlock("key1");
        template.unlock("key2");
    }

    @Test
    void testTryLockWithTimeout() {
        assertTrue(template.tryLock("key3", 100, TimeUnit.MILLISECONDS));
        template.unlock("key3");
    }

    @Test
    void testLock() {
        template.lock("key4");
        assertTrue(template.isLocked("key4"));
        template.unlock("key4");
        assertFalse(template.isLocked("key4"));
    }

    @Test
    void testUnlockWhenNotHeld() {
        assertDoesNotThrow(() -> template.unlock("nonExistentLock"));
    }

    @Test
    void testIsLocked() {
        assertFalse(template.isLocked("unknown"));
        template.lock("checkLock");
        assertTrue(template.isLocked("checkLock"));
        template.unlock("checkLock");
        assertFalse(template.isLocked("checkLock"));
    }

    @Test
    void testLock4jApiLock() {
        LockInfo info = template.lock("lock4j-key", 1000, 500, null);
        assertNotNull(info);
        assertEquals("lock4j-key", info.getLockKey());
        assertEquals(1000, info.getExpire());
        assertEquals(500, info.getAcquireTimeout());
        assertEquals(1, info.getAcquireCount());
        assertNotNull(info.getLockValue());

        assertTrue(template.releaseLock(info));
    }

    @Test
    void testLock4jApiReleaseNullLockInfo() {
        assertFalse(template.releaseLock(null));
    }

    @Test
    void testConcurrentLockExclusion() throws InterruptedException {
        int threadCount = 5;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger concurrentCount = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                template.execute("concurrent-lock", () -> {
                    int current = concurrentCount.incrementAndGet();
                    maxConcurrent.updateAndGet(m -> Math.max(m, current));
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    concurrentCount.decrementAndGet();
                    return null;
                });
                latch.countDown();
            });
        }
        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // 闂傚倷鐒﹂惇褰掑垂绾懌浜瑰璺虹焷婵櫕銇勯幒鎴濐仼闁?JVM 缂傚倸鍊烽懗鍓佲偓姘箻瀹曟﹢濡歌婵悂姊婚崒娆戭槮闁靛洦顭囧▎銏ゅΧ閸ヮ煈娼熷┑鐘虫磵閺嗗敎Concurrent 闂備礁婀遍崢褔鎮洪妸銉庢稒绗熼埀顒勩€佸顒夋僵妞ゆ挾濮弸?1
        assertEquals(1, maxConcurrent.get());
    }
}
