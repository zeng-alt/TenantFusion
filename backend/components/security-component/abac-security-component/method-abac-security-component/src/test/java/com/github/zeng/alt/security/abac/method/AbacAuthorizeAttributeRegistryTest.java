package com.github.zeng.alt.security.abac.method;

import com.github.zeng.alt.security.abac.AbacAuthorize;
import io.vavr.control.Option;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 注解注册表：方法/类级解析、MethodClassKey 缓存与无注解哨兵。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AbacAuthorizeAttributeRegistryTest {

    private final AbacAuthorizeAttributeRegistry registry = new AbacAuthorizeAttributeRegistry();

    @AbacAuthorize(permission = "user:update")
    static class AnnotatedService {
        public void update(Long id) {
        }

        @AbacAuthorize(
                permission = "user:delete",
                requirePolicy = true
        )
        public void delete(Long id) {
        }
    }

    static class PlainService {
        public void plain() {
        }
    }

    @Test
    void shouldResolveMethodAnnotation() throws Exception {
        final Method method = AnnotatedService.class.getMethod("delete", Long.class);
        final Option<AbacAuthorizeAttribute> attribute = registry.getAttribute(method, AnnotatedService.class);

        assertThat(attribute.get().permission()).isEqualTo("user:delete");
        assertThat(attribute.get().requirePolicy()).isTrue();
    }

    @Test
    void shouldFallBackToClassAnnotation() throws Exception {
        final Method method = AnnotatedService.class.getMethod("update", Long.class);
        final Option<AbacAuthorizeAttribute> attribute = registry.getAttribute(method, AnnotatedService.class);

        assertThat(attribute.get().permission()).isEqualTo("user:update");
        assertThat(attribute.get().requirePolicy()).isFalse();
    }

    @Test
    void shouldReturnNoneWithoutAnnotation() throws Exception {
        final Method method = PlainService.class.getMethod("plain");

        assertThat(registry.getAttribute(method, PlainService.class).isEmpty()).isTrue();
    }

    @Test
    void shouldCacheAttributePerMethodClassKey() throws Exception {
        final Method method = AnnotatedService.class.getMethod("delete", Long.class);
        final AbacAuthorizeAttribute first = registry.getAttribute(method, AnnotatedService.class).get();
        final AbacAuthorizeAttribute second = registry.getAttribute(method, AnnotatedService.class).get();

        assertThat(first).isSameAs(second);
    }
}