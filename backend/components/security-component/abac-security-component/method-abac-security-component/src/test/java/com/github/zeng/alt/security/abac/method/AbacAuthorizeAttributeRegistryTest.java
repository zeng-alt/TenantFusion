package com.github.zeng.alt.security.abac.method;

import com.github.zeng.alt.security.abac.AbacAuthorize;
import io.vavr.control.Option;
import org.junit.jupiter.api.Test;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

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

    interface GenericService<T> {
        T save(T input);
    }

    static class StringService implements GenericService<String> {
        @Override
        @AbacAuthorize(permission = "save")
        public String save(String input) {
            return input;
        }
    }

    @Test
    void shouldResolveImplementationAnnotationThroughGenericInterfaceMethod() throws Exception {
        final Method method = GenericService.class.getMethod("save", Object.class);
        assertThat(registry.getAttribute(method, StringService.class).get().permission()).isEqualTo("save");
    }

    @Test
    void resolvesParameterAndReturnMetadataOnceAcrossConcurrentReads() throws Exception {
        final AtomicInteger resolutions = new AtomicInteger();
        final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer() {
            @Override
            public String[] getParameterNames(Method method) {
                resolutions.incrementAndGet();
                return super.getParameterNames(method);
            }
        };
        final AbacAuthorizeAttributeRegistry concurrent = new AbacAuthorizeAttributeRegistry(discoverer);
        final Method method = GenericService.class.getMethod("save", Object.class);
        final List<AbacAuthorizeAttribute> attributes = IntStream.range(0, 1_000).parallel()
                .mapToObj(ignored -> concurrent.getAttribute(method, StringService.class).get()).toList();
        final AbacMethodDto metadata = attributes.getFirst().metadata();
        assertThat(attributes).allMatch(attribute -> attribute == attributes.getFirst());
        assertThat(resolutions).hasValue(1);
        assertThat(metadata.method()).isEqualTo(StringService.class.getMethod("save", String.class));
        assertThat(metadata.targetClass()).isEqualTo(StringService.class);
        assertThat(metadata.argumentIndices()).containsEntry("input", 0).containsEntry("p0", 0).containsEntry("a0", 0);
        assertThat(metadata.parameterTypes().getFirst().resolve()).isEqualTo(String.class);
        assertThat(metadata.returnType().resolve()).isEqualTo(String.class);
    }

    @Test
    void keepsPositionalAliasesWhenParameterNamesAreUnavailable() throws Exception {
        final ParameterNameDiscoverer unnamed = new ParameterNameDiscoverer() {
            @Override
            public String[] getParameterNames(Method method) { return null; }
            @Override
            public String[] getParameterNames(Constructor<?> constructor) { return null; }
        };
        final AbacMethodDto metadata = new AbacAuthorizeAttributeRegistry(unnamed)
                .getAttribute(StringService.class.getMethod("save", String.class), StringService.class).get().metadata();
        assertThat(metadata.argumentIndices()).containsOnlyKeys("p0", "a0");
    }

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
