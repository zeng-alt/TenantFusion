package com.github.zeng.alt.lock;

import com.github.zeng.alt.lock.model.DefaultLockKeyBuilder;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 闂傚倸鍊风粈渚€骞夐敓鐘偓鍐幢濡炴洘妞藉浠嬵敇閻愭彃浜堕梻浣筋潐瀹曟绮旈鈧畷鐑筋敇濞戞ü澹曢梺鎸庣箓妤犳悂鐛Ο璁崇箚闁告瑥顦伴崐鎰版煛鐏炶濡奸柍钘夘槸閳诲骸螣閸濆嫬顥沞faultLockKeyBuilder
 *
 * @author zengJiaJun
 * @since 2026婵?6闂?1闂?
 * @version 1.0
 */
class DefaultLockKeyBuilderTest {

    private final MethodBasedExpressionEvaluator evaluator = mock(MethodBasedExpressionEvaluator.class);
    private final DefaultLockKeyBuilder builder = new DefaultLockKeyBuilder(evaluator);

    @Test
    void testEmptyKeys() throws Exception {
        MethodInvocation invocation = mock(MethodInvocation.class);
        Method method = getClass().getDeclaredMethod("testEmptyKeys");
        when(invocation.getMethod()).thenReturn(method);
        when(invocation.getArguments()).thenReturn(new Object[]{});

        String key = builder.buildKey(invocation, new String[]{});
        assertEquals("", key);
    }

    @Test
    void testNullKeys() throws Exception {
        MethodInvocation invocation = mock(MethodInvocation.class);
        Method method = getClass().getDeclaredMethod("testNullKeys");
        when(invocation.getMethod()).thenReturn(method);
        when(invocation.getArguments()).thenReturn(new Object[]{});

        String key = builder.buildKey(invocation, null);
        assertEquals("", key);
    }

    @Test
    void testSingleKey() throws Exception {
        MethodInvocation invocation = mock(MethodInvocation.class);
        Method method = getClass().getDeclaredMethod("testSingleKey");
        when(invocation.getMethod()).thenReturn(method);
        when(invocation.getArguments()).thenReturn(new Object[]{});
        when(evaluator.getValue(method, new Object[]{}, "#p0", String.class)).thenReturn("userId");

        String key = builder.buildKey(invocation, new String[]{"#p0"});
        assertEquals("userId", key);
    }

    @Test
    void testMultipleKeysJoinedByDot() throws Exception {
        MethodInvocation invocation = mock(MethodInvocation.class);
        Method method = getClass().getDeclaredMethod("testMultipleKeysJoinedByDot");
        when(invocation.getMethod()).thenReturn(method);
        when(invocation.getArguments()).thenReturn(new Object[]{});
        when(evaluator.getValue(method, new Object[]{}, "#p0", String.class)).thenReturn("user");
        when(evaluator.getValue(method, new Object[]{}, "#p1", String.class)).thenReturn("123");

        String key = builder.buildKey(invocation, new String[]{"#p0", "#p1"});
        assertEquals("user.123", key);
    }

    @Test
    void testFiltersBlankKeys() throws Exception {
        MethodInvocation invocation = mock(MethodInvocation.class);
        Method method = getClass().getDeclaredMethod("testFiltersBlankKeys");
        when(invocation.getMethod()).thenReturn(method);
        when(invocation.getArguments()).thenReturn(new Object[]{});
        when(evaluator.getValue(method, new Object[]{}, "valid", String.class)).thenReturn("result");

        String key = builder.buildKey(invocation, new String[]{"", "valid", null});
        assertEquals("result", key);
    }
}
