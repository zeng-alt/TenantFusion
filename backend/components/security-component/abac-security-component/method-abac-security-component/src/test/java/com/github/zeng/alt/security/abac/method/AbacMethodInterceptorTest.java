package com.github.zeng.alt.security.abac.method;

import com.github.zeng.alt.security.abac.AbacAuthorizationBuilder;
import com.github.zeng.alt.security.abac.AbacAuthorize;
import com.github.zeng.alt.security.abac.AbacContext;
import com.github.zeng.alt.security.abac.AbacFailure;
import com.github.zeng.alt.security.abac.AbacMethodContext;
import com.github.zeng.alt.security.abac.AbacResourceResolver;
import com.github.zeng.alt.security.abac.AbacTemplate;
import io.vavr.control.Either;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.security.access.AccessDeniedException;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 拦截器：fail-closed 拒绝、按注解声明从容器取解析器、参数自动注入、解析器合并与 requirePolicy 透传。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AbacMethodInterceptorTest {

    private final AbacAuthorizeAttributeRegistry registry = new AbacAuthorizeAttributeRegistry();
    private final AbacTemplate template = mock(AbacTemplate.class);
    private final AbacSubjectProvider subjectProvider = mock(AbacSubjectProvider.class);
    private final AbacEnvironmentProvider environmentProvider = context -> Map.of("action", "PATCH");

    static class TestResolver implements AbacResourceResolver {
        private final Map<String, Object> attributes;
        private final RuntimeException failure;
        AbacMethodContext captured;

        TestResolver(Map<String, Object> attributes) {
            this(attributes, null);
        }

        TestResolver(Map<String, Object> attributes, RuntimeException failure) {
            this.attributes = attributes;
            this.failure = failure;
        }

        @Override
        public Map<String, Object> resolve(AbacMethodContext context) {
            captured = context;
            if (failure != null) {
                throw failure;
            }
            return attributes;
        }
    }

    static class FirstResolver implements AbacResourceResolver {
        @Override
        public Map<String, Object> resolve(AbacMethodContext context) {
            return Map.of("id", "42", "deptId", 1L);
        }
    }

    static class SecondResolver implements AbacResourceResolver {
        @Override
        public Map<String, Object> resolve(AbacMethodContext context) {
            return Map.of("deptId", 2L);
        }
    }

    static class UserService {
        @AbacAuthorize(
                permission = "user:update",
                resources = {TestResolver.class}
        )
        public Long patchUser(Long id, String name) {
            return id;
        }

        @AbacAuthorize(
                permission = "user:delete",
                requirePolicy = true,
                resources = {TestResolver.class}
        )
        public void delete(Long id) {
        }

        @AbacAuthorize(
                permission = "user:update",
                resources = {FirstResolver.class, SecondResolver.class}
        )
        public void merge(Long id) {
        }

        public void plain() {
        }
    }

    private AbacMethodInterceptor interceptor(ListableBeanFactory beans) {
        return new AbacMethodInterceptor(registry, subjectProvider, environmentProvider, beans, template);
    }

    private StaticListableBeanFactory beans(AbacResourceResolver resolver) {
        final StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("testResolver", resolver);
        return beans;
    }

    private MethodInvocation invocation(String methodName, Class<?>[] types, Object[] args) throws Throwable {
        final Method method = UserService.class.getMethod(methodName, types);
        final MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getMethod()).thenReturn(method);
        when(invocation.getThis()).thenReturn(new UserService());
        when(invocation.getArguments()).thenReturn(args);
        when(invocation.proceed()).thenReturn("PROCEEDED");
        return invocation;
    }

    private void stubSubject() {
        when(subjectProvider.resolve(any())).thenReturn(Either.right(new AbacSubject("t001", Map.of("deptId", 10L), java.util.Map.of("authenticated", true))));
    }

    private void stubDecision(Either<AbacFailure, Boolean> decision) {
        when(template.authorize(any())).thenReturn(new AbacAuthorizationBuilder(required -> decision));
    }

    @Test
    void shouldProceedWhenGranted() throws Throwable {
        stubSubject();
        stubDecision(Either.right(true));
        final MethodInvocation invocation = invocation("patchUser", new Class<?>[]{Long.class, String.class},
                new Object[]{42L, "bob"});

        assertThat(interceptor(beans(new TestResolver(Map.of("id", "42", "deptId", 10L)))).invoke(invocation))
                .isEqualTo("PROCEEDED");
        final ArgumentCaptor<AbacContext> captor = ArgumentCaptor.forClass(AbacContext.class);
        verify(template, org.mockito.Mockito.times(2)).authorize(captor.capture());
        assertThat(captor.getValue().tenant()).isEqualTo("t001");
        assertThat(captor.getValue().permission()).isEqualTo("user:update");
        assertThat(captor.getValue().resource().get("id")).isEqualTo("42");
        assertThat(captor.getValue().env().get("action")).isEqualTo("PATCH");
    }

    @Test
    void shouldProceedWithoutAnnotationAndSkipProviders() throws Throwable {
        final MethodInvocation invocation = invocation("plain", new Class<?>[]{}, new Object[]{});

        assertThat(interceptor(new StaticListableBeanFactory()).invoke(invocation)).isEqualTo("PROCEEDED");
        verify(subjectProvider, never()).resolve(any());
        verify(template, never()).authorize(any());
    }

    @Test
    void shouldDenyWhenSubjectResolutionFails() throws Throwable {
        when(subjectProvider.resolve(any())).thenReturn(Either.left("当前用户未登录"));
        final MethodInvocation invocation = invocation("patchUser", new Class<?>[]{Long.class, String.class},
                new Object[]{42L, "bob"});

        assertThatThrownBy(() -> interceptor(beans(new TestResolver(Map.of("id", "42")))).invoke(invocation))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("当前用户未登录");
        verify(template, never()).authorize(any());
    }

    @Test
    void shouldDenyWhenPolicyReturnsFalse() throws Throwable {
        stubSubject();
        stubDecision(Either.right(false));
        final MethodInvocation invocation = invocation("patchUser", new Class<?>[]{Long.class, String.class},
                new Object[]{42L, "bob"});

        assertThatThrownBy(() -> interceptor(beans(new TestResolver(Map.of("id", "42")))).invoke(invocation))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldDenyWithFailureMessageWhenAuthorizationFails() throws Throwable {
        stubSubject();
        stubDecision(Either.left(new AbacFailure("POLICY_DENIED", "不满足资源访问条件")));
        final MethodInvocation invocation = invocation("patchUser", new Class<?>[]{Long.class, String.class},
                new Object[]{42L, "bob"});

        assertThatThrownBy(() -> interceptor(beans(new TestResolver(Map.of("id", "42")))).invoke(invocation))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("不满足资源访问条件");
    }

    @Test
    void shouldPassRequirePolicyToBuilder() throws Throwable {
        stubSubject();
        final AtomicBoolean required = new AtomicBoolean(false);
        when(template.authorize(any())).thenReturn(new AbacAuthorizationBuilder(flag -> {
            required.set(flag);
            return Either.right(true);
        }));
        final MethodInvocation invocation = invocation("delete", new Class<?>[]{Long.class}, new Object[]{42L});

        interceptor(beans(new TestResolver(Map.of("id", "42")))).invoke(invocation);

        assertThat(required.get()).isTrue();
    }

    @Test
    void shouldInjectMethodParametersByNameAndPosition() throws Throwable {
        stubSubject();
        stubDecision(Either.right(true));
        final TestResolver resolver = new TestResolver(Map.of("id", "42"));
        final MethodInvocation invocation = invocation("patchUser", new Class<?>[]{Long.class, String.class},
                new Object[]{42L, "bob"});

        interceptor(beans(resolver)).invoke(invocation);

        assertThat(resolver.captured.paramByName().get("id")).isEqualTo(42L);
        assertThat(resolver.captured.paramByName().get("p0")).isEqualTo(42L);
        assertThat(resolver.captured.paramByName().get("a0")).isEqualTo(42L);
        assertThat(resolver.captured.paramByName().get("name")).isEqualTo("bob");
        assertThat(resolver.captured.permission()).isEqualTo("user:update");
    }

    @Test
    void bindsFreshValuesForEachInvocationWhileReusingMetadata() throws Throwable {
        stubSubject();
        stubDecision(Either.right(true));
        final TestResolver resolver = new TestResolver(Map.of("id", "42"));
        final AbacMethodInterceptor interceptor = interceptor(beans(resolver));
        interceptor.invoke(invocation("patchUser", new Class<?>[]{Long.class, String.class}, new Object[]{42L, "bob"}));
        final AbacMethodContext first = resolver.captured;
        interceptor.invoke(invocation("patchUser", new Class<?>[]{Long.class, String.class}, new Object[]{43L, "alice"}));
        assertThat(first.paramByName()).containsEntry("id", 42L).containsEntry("name", "bob");
        assertThat(resolver.captured.paramByName()).containsEntry("id", 43L).containsEntry("name", "alice");
    }

    @Test
    void shouldMergeResolversInDeclarationOrderWithLaterOverriding() throws Throwable {
        stubSubject();
        stubDecision(Either.right(true));
        final StaticListableBeanFactory factory = new StaticListableBeanFactory();
        factory.addBean("first", new FirstResolver());
        factory.addBean("second", new SecondResolver());
        final MethodInvocation invocation = invocation("merge", new Class<?>[]{Long.class}, new Object[]{42L});

        interceptor(factory).invoke(invocation);

        final ArgumentCaptor<AbacContext> captor = ArgumentCaptor.forClass(AbacContext.class);
        verify(template, org.mockito.Mockito.times(2)).authorize(captor.capture());
        assertThat(captor.getValue().resource().get("deptId")).isEqualTo(2L);
        assertThat(captor.getValue().resource().get("id")).isEqualTo("42");
    }

    @Test
    void shouldFailClosedWhenResolverThrows() throws Throwable {
        stubSubject();
        final MethodInvocation invocation = invocation("patchUser", new Class<?>[]{Long.class, String.class},
                new Object[]{42L, "bob"});

        assertThatThrownBy(() -> interceptor(beans(new TestResolver(Map.of(), new IllegalStateException("boom"))))
                .invoke(invocation)).isInstanceOf(AccessDeniedException.class);
        verify(template, never()).authorize(any());
    }

    @Test
    void shouldFailClosedWhenDeclaredResolverIsNotABean() throws Throwable {
        stubSubject();
        final MethodInvocation invocation = invocation("patchUser", new Class<?>[]{Long.class, String.class},
                new Object[]{42L, "bob"});

        assertThatThrownBy(() -> interceptor(new StaticListableBeanFactory()).invoke(invocation))
                .isInstanceOf(AccessDeniedException.class);
        verify(template, never()).authorize(any());
    }
}