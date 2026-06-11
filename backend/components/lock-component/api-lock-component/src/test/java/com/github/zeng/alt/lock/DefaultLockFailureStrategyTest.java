package com.github.zeng.alt.lock;

import com.github.zeng.alt.lock.exception.LockFailureException;
import com.github.zeng.alt.lock.model.DefaultLockFailureStrategy;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 闂傚倸鍊风粈渚€骞夐敓鐘偓鍐幢濡炴洘妞藉浠嬵敇閻愭彃浜堕梻浣筋潐瀹曟绮旈鈧畷鐑筋敇濞戞ü澹曢梺鎸庣箓妤犳悂鐛Ο璁崇箚闁告瑥顦伴崐鎰版煛鐏炶濡奸柍钘夘槸閳诲骸螣閸濆嫬顥沞faultLockFailureStrategy
 *
 * @author zengJiaJun
 * @since 2026婵?6闂?1闂?
 * @version 1.0
 */
class DefaultLockFailureStrategyTest {

    private final DefaultLockFailureStrategy strategy = new DefaultLockFailureStrategy();

    @Test
    void testOnLockFailureThrowsLockFailureException() throws Throwable {
        Method method = getClass().getDeclaredMethod("testOnLockFailureThrowsLockFailureException");
        LockFailureException thrown = assertThrows(LockFailureException.class,
                () -> strategy.onLockFailure("testKey", method, new Object[]{}));
        assertEquals(DefaultLockFailureStrategy.DEFAULT_MESSAGE, thrown.getMessage());
    }

    @Test
    void testConstantMessageValue() {
        assertEquals("request failed, please retry it.", DefaultLockFailureStrategy.DEFAULT_MESSAGE);
    }
}
