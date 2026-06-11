package com.github.zeng.alt.lock;

import com.github.zeng.alt.lock.api.DistributedLock;
import com.github.zeng.alt.lock.api.NoOpDistributedLock;
import com.github.zeng.alt.lock.api.NoOpLockTemplate;
import com.github.zeng.alt.lock.exception.LockException;
import com.github.zeng.alt.lock.exception.LockFailureException;
import com.github.zeng.alt.lock.executor.AbstractLockExecutor;
import com.github.zeng.alt.lock.model.LockInfo;
import com.github.zeng.alt.lock.config.LockProperties;
import com.github.zeng.alt.lock.model.LockUtils;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 闂傚倸鍊风粈渚€骞夐敓鐘偓鍐幢濡炴洘妞藉浠嬵敇閻愭彃浜堕梻浣筋潐瀹曟绮旈鈧畷鐑筋敇濞戞ü澹曢梺鎸庣箓妤犳悂鐛Ο璁崇箚闁告瑥顦伴崐鎰版煛鐏炶濡奸柍钘夘槸閳诲酣骞嬮悙鏉戞珝pi-lock-component 濠电姷鏁搁崑鐐哄垂閸洖绠归柍鍝勬噹閸屻劑鏌熼鍡忓亾闁?POJO / 闂備浇顕у锕傦綖婢舵劕绠栭柛顐ｆ礀绾惧潡姊洪鈧粔鎾儗?/ 缂傚倸鍊搁崐椋庣矆娓氣偓閹崇喖顢涢悙鑼啇濡炪倖鍔ч梽鍕磻椤忓牊鐓曢柡鍥ュ妼閻忥繝鏌涚€ｎ偆绠為柡宀€鍠庨埢鎾诲垂椤旂晫浠屾俊?
 *
 * @author zengJiaJun
 * @since 2026婵?6闂?1闂?
 * @version 1.0
 */
class ApiLockUnitTest {

    // ========== LockInfo ==========

    @Test
    void testLockInfoConstructorAndGetters() {
        Object instance = new Object();
        LockInfo info = new LockInfo("myKey", "myValue", 30000L, 3000L, 1, instance, null);

        assertEquals("myKey", info.getLockKey());
        assertEquals("myValue", info.getLockValue());
        assertEquals(30000L, info.getExpire());
        assertEquals(3000L, info.getAcquireTimeout());
        assertEquals(1, info.getAcquireCount());
        assertSame(instance, info.getLockInstance());
        assertNull(info.getLockExecutor());
    }

    @Test
    void testLockInfoToString() {
        LockInfo info = new LockInfo("key1", "val1", 1000L, 500L, 2, null, null);
        String str = info.toString();
        assertTrue(str.contains("lockKey='key1'"));
        assertTrue(str.contains("expire=1000"));
        assertTrue(str.contains("acquireTimeout=500"));
        assertTrue(str.contains("acquireCount=2"));
    }

    // ========== LockUtils ==========

    @Test
    void testSimpleUUID() {
        String uuid = LockUtils.simpleUUID();
        assertNotNull(uuid);
        assertEquals(32, uuid.length());
        assertFalse(uuid.contains("-"));
    }

    @Test
    void testSimpleUUIDIsUnique() {
        String uuid1 = LockUtils.simpleUUID();
        String uuid2 = LockUtils.simpleUUID();
        assertNotEquals(uuid1, uuid2);
    }

    // ========== LockProperties ==========

    @Test
    void testLockPropertiesDefaults() {
        LockProperties props = new LockProperties();
        assertEquals(30000L, props.getExpire());
        assertEquals(3000L, props.getAcquireTimeout());
        assertEquals(100L, props.getRetryInterval());
        assertEquals("alt:lock", props.getLockKeyPrefix());
        assertNull(props.getPrimaryExecutor());
        assertNull(props.getPrimaryFailStrategy());
        assertNull(props.getPrimaryKeyBuilder());
    }

    @Test
    void testLockPropertiesSetters() {
        LockProperties props = new LockProperties();
        props.setExpire(1000L);
        props.setAcquireTimeout(500L);
        props.setRetryInterval(50L);
        props.setLockKeyPrefix("my:prefix");

        assertEquals(1000L, props.getExpire());
        assertEquals(500L, props.getAcquireTimeout());
        assertEquals(50L, props.getRetryInterval());
        assertEquals("my:prefix", props.getLockKeyPrefix());
    }

    // ========== LockException / LockFailureException ==========

    @Test
    void testLockExceptionNoArg() {
        LockException ex = new LockException();
        assertNull(ex.getMessage());
    }

    @Test
    void testLockExceptionWithMessage() {
        LockException ex = new LockException("test error");
        assertEquals("test error", ex.getMessage());
    }

    @Test
    void testLockExceptionWithCause() {
        Throwable cause = new RuntimeException("cause");
        LockException ex = new LockException(cause);
        assertSame(cause, ex.getCause());
    }

    @Test
    void testLockExceptionWithMessageAndCause() {
        Throwable cause = new RuntimeException("cause");
        LockException ex = new LockException("msg", cause);
        // BaseException(String title, Throwable) maps "msg" to title, message=throwable.toString()
        assertSame(cause, ex.getCause());
    }

    @Test
    void testLockFailureExceptionNoArg() {
        LockFailureException ex = new LockFailureException();
        assertNull(ex.getMessage());
    }

    @Test
    void testLockFailureExceptionWithMessage() {
        LockFailureException ex = new LockFailureException("fail");
        assertEquals("fail", ex.getMessage());
    }

    @Test
    void testLockFailureExceptionWithMessageAndCause() {
        Throwable cause = new RuntimeException("cause");
        LockFailureException ex = new LockFailureException("msg", cause);
        // BaseException(String title, Throwable) maps "msg" to title, message=throwable.toString()
        assertSame(cause, ex.getCause());
    }

    @Test
    void testLockFailureExtendsLockException() {
        assertInstanceOf(LockException.class, new LockFailureException());
    }

    // ========== AbstractLockExecutor ==========

    @Test
    void testObtainLockInstanceReturnsInstanceWhenLocked() {
        TestLockExecutor executor = new TestLockExecutor();
        String instance = executor.callObtainLockInstance(true, "myLock");
        assertEquals("myLock", instance);
    }

    @Test
    void testObtainLockInstanceReturnsNullWhenNotLocked() {
        TestLockExecutor executor = new TestLockExecutor();
        String instance = executor.callObtainLockInstance(false, "myLock");
        assertNull(instance);
    }

    /**
     * 闂傚倷绀侀幖顐λ囬鐐村亱闁糕剝绋戠粻鐘诲箹濞ｎ剙鐏柛娆忕箲閵囧嫯绠涢幘鏂ユ灆闂侀潻瀵岄崢鎼佸磻閹剧粯鏅查幖绮光偓宕囶啋婵犵鍓濊ぐ鍐箺濠婂懏顫曢柟鐑橆殢閺佸啴鏌ㄥ☉妯侯仹閻熸洖妫涚槐鎾存媴娴犲鎽电紓浣筋嚙閻楀棝顢氶敐澶婄闁芥ê顦遍鎺戭渻閵堝棙鐓ラ柍褜鍓涢崰搴ㄦ偟?protected 闂傚倸鍊风粈渚€骞栭锕€纾瑰ù鐘差儜缂嶆牕顭跨捄鐑樻拱闁?
     */
    private static class TestLockExecutor extends AbstractLockExecutor<String> {
        @Override
        public String acquire(String lockKey, String lockValue, long expire, long acquireTimeout) {
            return null;
        }

        @Override
        public boolean releaseLock(String key, String value, String lockInstance) {
            return false;
        }

        public String callObtainLockInstance(boolean locked, String lockInstance) {
            return obtainLockInstance(locked, lockInstance);
        }
    }


    // ========== NoOpDistributedLock ==========

    @Test
    void testNoOpDistributedLock() {
        DistributedLock lock = new NoOpDistributedLock("testLock");

        assertEquals("testLock", lock.name());
        assertTrue(lock.tryLock());
        assertTrue(lock.tryLock(100, TimeUnit.MILLISECONDS));
        assertTrue(lock.tryLock(100, 200, TimeUnit.MILLISECONDS));
        assertDoesNotThrow(lock::lock);
        assertDoesNotThrow(lock::unlock);
        assertTrue(lock.isHeldByCurrentThread());
        assertFalse(lock.isLocked());
    }

    @Test
    void testNoOpDistributedLockClose() {
        DistributedLock lock = new NoOpDistributedLock("closeTest");
        assertDoesNotThrow(lock::close);
    }

    // ========== NoOpLockTemplate ==========

    @Test
    void testNoOpLockTemplateExecuteWithSupplier() {
        NoOpLockTemplate template = new NoOpLockTemplate();
        String result = template.execute("lock", () -> "hello");
        assertEquals("hello", result);
    }

    @Test
    void testNoOpLockTemplateExecuteWithRunnable() {
        NoOpLockTemplate template = new NoOpLockTemplate();
        int[] counter = {0};
        template.execute("lock", () -> counter[0]++);
        assertEquals(1, counter[0]);
    }

    @Test
    void testNoOpLockTemplateExecuteWithTimeoutAndSupplier() {
        NoOpLockTemplate template = new NoOpLockTemplate();
        String result = template.execute("lock", 100, 200, TimeUnit.MILLISECONDS, () -> "timed");
        assertEquals("timed", result);
    }

    @Test
    void testNoOpLockTemplateExecuteWithTimeoutAndRunnable() {
        NoOpLockTemplate template = new NoOpLockTemplate();
        int[] counter = {0};
        template.execute("lock", 100, 200, TimeUnit.MILLISECONDS, () -> counter[0]++);
        assertEquals(1, counter[0]);
    }

    @Test
    void testNoOpLockTemplateGetLock() {
        NoOpLockTemplate template = new NoOpLockTemplate();
        DistributedLock lock = template.getLock("myLock");
        assertInstanceOf(NoOpDistributedLock.class, lock);
        assertEquals("myLock", lock.name());
    }

    @Test
    void testNoOpLockTemplateGetFairLock() {
        NoOpLockTemplate template = new NoOpLockTemplate();
        DistributedLock lock = template.getFairLock("fairLock");
        assertInstanceOf(NoOpDistributedLock.class, lock);
        assertEquals("fairLock", lock.name());
    }

    @Test
    void testNoOpLockTemplateDirectOperations() {
        NoOpLockTemplate template = new NoOpLockTemplate();
        assertTrue(template.tryLock("key"));
        assertTrue(template.tryLock("key", 100, TimeUnit.MILLISECONDS));
        assertDoesNotThrow(() -> template.lock("key"));
        assertDoesNotThrow(() -> template.unlock("key"));
        assertFalse(template.isLocked("key"));
    }

    @Test
    void testNoOpLockTemplateLock4jApi() {
        NoOpLockTemplate template = new NoOpLockTemplate();
        LockInfo info = template.lock("key", 1000, 500, null);
        assertNotNull(info);
        assertEquals("key", info.getLockKey());
        assertEquals(1000, info.getExpire());
        assertEquals(500, info.getAcquireTimeout());
        assertEquals(1, info.getAcquireCount());

        assertTrue(template.releaseLock(info));
        assertTrue(template.releaseLock(null));
    }
}

