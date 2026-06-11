package com.github.zeng.alt.lock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.util.StringValueResolver;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 鍗曞厓娴嬭瘯锛歋pelMethodBasedExpressionEvaluator
 *
 * @author zengJiaJun
 * @since 2026骞?6鏈?1鏃?
 * @version 1.0
 */
class SpelMethodBasedExpressionEvaluatorTest {

    private SpelMethodBasedExpressionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new SpelMethodBasedExpressionEvaluator();
        BeanFactory beanFactory = new StaticApplicationContext();
        evaluator.setBeanFactory(beanFactory);
        evaluator.setEmbeddedValueResolver(new StringValueResolver() {
            @Override
            public String resolveStringValue(String strVal) {
                return strVal;
            }
        });
    }

    @Test
    void testEvaluateByParameterIndex() throws Exception {
        Method method = getClass().getMethod("sampleMethod", String.class, int.class);
        Object[] args = {"hello", 42};

        String result = evaluator.getValue(method, args, "#p0", String.class);
        assertEquals("hello", result);
    }

    @Test
    void testEvaluateByParameterIndexAlt() throws Exception {
        Method method = getClass().getMethod("sampleMethod", String.class, int.class);
        Object[] args = {"world", 99};

        Integer result = evaluator.getValue(method, args, "#a1", Integer.class);
        assertEquals(99, result);
    }

    @Test
    void testEvaluateByParameterName() throws Exception {
        // 浣跨敤甯﹀弬鏁板悕鐨勬柟娉?
        Method method = getClass().getMethod("namedMethod", String.class, String.class);
        Object[] args = {"key1", "value1"};

        // 鐢变簬 DefaultParameterNameDiscoverer 瀵归潪缂栬瘧璋冭瘯淇℃伅鐨勬柟娉曡繑鍥?arg0, arg1...
        // 鎴戜滑浣跨敤 #a0, #a1 浣滀负鏇夸唬
        String result = evaluator.getValue(method, args, "#a0", String.class);
        assertEquals("key1", result);
    }

    @Test
    void testEvaluateWithCustomVariables() throws Exception {
        Method method = getClass().getMethod("sampleMethod", String.class, int.class);
        Object[] args = {"test", 10};

        String result = evaluator.getValue(method, args, "#customVar", String.class,
                Map.of("customVar", "customValue"));
        assertEquals("customValue", result);
    }

    @Test
    void testEvaluateMapAccessor() throws Exception {
        Method method = getClass().getMethod("sampleMethod", String.class, int.class);
        Object[] args = {"mapTest", 5};

        String result = evaluator.getValue(method, args, "#a0?.length()", String.class);
        assertEquals("7", result);
    }

    @Test
    void testExpressionCaching() throws Exception {
        Method method = getClass().getMethod("sampleMethod", String.class, int.class);
        Object[] args = {"cache", 1};

        String result1 = evaluator.getValue(method, args, "#p0", String.class);
        String result2 = evaluator.getValue(method, args, "#p0", String.class);
        assertEquals("cache", result1);
        assertEquals("cache", result2);
    }

    @Test
    void testEvaluateWithRootObject() throws Exception {
        Method method = getClass().getMethod("sampleMethod", String.class, int.class);
        Object[] args = {"root", 7};

        String methodName = evaluator.getValue(method, args, "#root.name", String.class);
        assertEquals("sampleMethod", methodName);
    }

    @SuppressWarnings("unused")
    public void sampleMethod(String str, int num) {
    }

    @SuppressWarnings("unused")
    public void namedMethod(String key, String value) {
    }
}
