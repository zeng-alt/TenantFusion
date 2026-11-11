package com.github.zeng.alt.security.core.web;

import com.github.zeng.alt.security.api.AuthorizationConstraint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * 验证同步组合授权的 OR 兼容性及 AND 约束。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class CompositeAuthorizationManagerTest {

    private final Supplier<Authentication> authentication =
            () -> new TestingAuthenticationToken("user", "unused", "ROLE_USER");
    private final RequestAuthorizationContext context =
            new RequestAuthorizationContext(new MockHttpServletRequest());

    @ParameterizedTest
    @CsvSource({"CHECK,false", "CHECK,true", "AUTHORIZE,false", "AUTHORIZE,true"})
    void bothConstructorsKeepOrAndStopAtFirstGrant(EntryPoint entryPoint, boolean withConstraints) {
        final List<String> calls = new ArrayList<>();
        final List<AuthorizationManager<RequestAuthorizationContext>> managers = List.of(
                base("abstain", null, calls),
                base("deny", false, calls),
                base("allow", true, calls),
                base("unreached", true, calls)
        );
        final var composite = withConstraints
                ? new CompositeAuthorizationManager(managers, List.of())
                : new CompositeAuthorizationManager(managers);

        assertThat(evaluate(composite, entryPoint).isGranted()).isTrue();
        assertThat(calls).containsExactly("abstain", "deny", "allow");
    }

    @ParameterizedTest
    @CsvSource({"CHECK,false", "CHECK,true", "AUTHORIZE,false", "AUTHORIZE,true"})
    void bothConstructorsDenyWithoutBaseManagers(EntryPoint entryPoint, boolean withConstraints) {
        final var composite = withConstraints
                ? new CompositeAuthorizationManager(List.of(), List.of())
                : new CompositeAuthorizationManager(List.of());

        assertThat(evaluate(composite, entryPoint).isGranted()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(EntryPoint.class)
    void grantsOnlyAfterAllConstraintsAllowInOrder(EntryPoint entryPoint) {
        final List<String> calls = new ArrayList<>();
        final var composite = new CompositeAuthorizationManager(
                List.of(base("base", true, calls)),
                List.of(constraint("first", true, calls), constraint("second", true, calls))
        );

        assertThat(evaluate(composite, entryPoint).isGranted()).isTrue();
        assertThat(calls).containsExactly("base", "first", "second");
    }

    @ParameterizedTest
    @EnumSource(EntryPoint.class)
    void constraintDenialStopsLaterConstraintsAndDoesNotResumeOr(EntryPoint entryPoint) {
        final List<String> calls = new ArrayList<>();
        final var composite = new CompositeAuthorizationManager(
                List.of(base("base", true, calls), base("laterBase", true, calls)),
                List.of(
                        constraint("first", true, calls),
                        constraint("deny", false, calls),
                        constraint("unreached", true, calls)
                )
        );

        assertThat(evaluate(composite, entryPoint).isGranted()).isFalse();
        assertThat(calls).containsExactly("base", "first", "deny");
    }

    @ParameterizedTest
    @EnumSource(EntryPoint.class)
    void nullConstraintDecisionDeniesAndStops(EntryPoint entryPoint) {
        final List<String> calls = new ArrayList<>();
        final var composite = new CompositeAuthorizationManager(
                List.of(base("base", true, calls)),
                List.of(constraint("null", null, calls), constraint("unreached", true, calls))
        );

        assertThat(evaluate(composite, entryPoint).isGranted()).isFalse();
        assertThat(calls).containsExactly("base", "null");
    }

    @ParameterizedTest
    @EnumSource(EntryPoint.class)
    void baseDenialDoesNotEvaluateConstraints(EntryPoint entryPoint) {
        final List<String> calls = new ArrayList<>();
        final var composite = new CompositeAuthorizationManager(
                List.of(base("deny", false, calls), base("abstain", null, calls)),
                List.of(constraint("unreached", true, calls))
        );

        assertThat(evaluate(composite, entryPoint).isGranted()).isFalse();
        assertThat(calls).containsExactly("deny", "abstain");
    }

    @ParameterizedTest
    @EnumSource(EntryPoint.class)
    void emptyBaseDoesNotEvaluateConstraints(EntryPoint entryPoint) {
        final List<String> calls = new ArrayList<>();
        final var composite = new CompositeAuthorizationManager(
                List.of(), List.of(constraint("unreached", true, calls))
        );

        assertThat(evaluate(composite, entryPoint).isGranted()).isFalse();
        assertThat(calls).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(EntryPoint.class)
    void baseExceptionPropagatesWithoutGrantingOrEvaluatingConstraints(EntryPoint entryPoint) {
        final var failure = new IllegalStateException("base failure");
        final List<String> calls = new ArrayList<>();
        final AuthorizationManager<RequestAuthorizationContext> broken = (auth, request) -> {
            throw failure;
        };
        final var composite = new CompositeAuthorizationManager(
                List.of(broken, base("unreachedBase", true, calls)),
                List.of(constraint("unreachedConstraint", true, calls))
        );

        assertThatThrownBy(() -> evaluate(composite, entryPoint)).isSameAs(failure);
        assertThat(calls).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(EntryPoint.class)
    void constraintExceptionPropagatesAndStops(EntryPoint entryPoint) {
        final var failure = new IllegalStateException("constraint failure");
        final List<String> calls = new ArrayList<>();
        final AuthorizationConstraint<RequestAuthorizationContext> broken = (auth, request) -> {
            calls.add("broken");
            throw failure;
        };
        final var composite = new CompositeAuthorizationManager(
                List.of(base("base", true, calls)),
                List.of(broken, constraint("unreached", true, calls))
        );

        assertThatThrownBy(() -> evaluate(composite, entryPoint)).isSameAs(failure);
        assertThat(calls).containsExactly("base", "broken");
    }

    @Test
    void authorizeKeepsCustomAuthorizationResultAndUsesAuthorizeOnConstraints() {
        final AuthorizationManager<RequestAuthorizationContext> manager = mock();
        final AuthorizationConstraint<RequestAuthorizationContext> constraint = mock();
        final AuthorizationResult granted = () -> true;
        when(manager.authorize(authentication, context)).thenReturn(granted);
        when(constraint.authorize(authentication, context)).thenReturn(new AuthorizationDecision(true));
        final var composite = new CompositeAuthorizationManager(List.of(manager), List.of(constraint));

        assertThat(composite.authorize(authentication, context)).isSameAs(granted);
        when(constraint.authorize(authentication, context)).thenReturn(null);
        assertThat(composite.authorize(authentication, context).isGranted()).isFalse();
        verify(manager, times(2)).authorize(authentication, context);
        verify(constraint, times(2)).authorize(authentication, context);
        verifyNoMoreInteractions(manager, constraint);
    }

    private AuthorizationResult evaluate(CompositeAuthorizationManager composite, EntryPoint entryPoint) {
        return switch (entryPoint) {
            case CHECK -> composite.check(authentication, context);
            case AUTHORIZE -> composite.authorize(authentication, context);
        };
    }

    private AuthorizationManager<RequestAuthorizationContext> base(
            String name, Boolean granted, List<String> calls
    ) {
        return (auth, request) -> {
            assertThat(auth).isSameAs(authentication);
            assertThat(request).isSameAs(context);
            calls.add(name);
            return granted == null ? null : new AuthorizationDecision(granted);
        };
    }

    private AuthorizationConstraint<RequestAuthorizationContext> constraint(
            String name, Boolean granted, List<String> calls
    ) {
        return (auth, request) -> {
            assertThat(auth).isSameAs(authentication);
            assertThat(request).isSameAs(context);
            calls.add(name);
            return granted == null ? null : new AuthorizationDecision(granted);
        };
    }

    private enum EntryPoint {
        CHECK, AUTHORIZE
    }
}
