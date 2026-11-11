package com.github.zeng.alt.security.core.reactive;

import com.github.zeng.alt.security.api.ReactiveAuthorizationConstraint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证响应式组合授权的顺序与短路语义。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class CompositeReactiveAuthorizationManagerTest {

    private final Mono<Authentication> authentication =
            Mono.just(new TestingAuthenticationToken("user", "unused", "ROLE_USER"));
    private final AuthorizationContext context = new AuthorizationContext(
            MockServerWebExchange.from(MockServerHttpRequest.get("/secured").build())
    );

    @Test
    void waitsForEarlierBaseManagerAndStopsAfterItsGrant() {
        final var pending = TestPublisher.<AuthorizationDecision>create();
        final List<String> calls = new ArrayList<>();
        final ReactiveAuthorizationManager<AuthorizationContext> first = (auth, request) -> {
            calls.add("first");
            return pending.mono();
        };
        final ReactiveAuthorizationManager<AuthorizationContext> later = (auth, request) -> {
            calls.add("later");
            return Mono.just(new AuthorizationDecision(true));
        };
        final var composite = new CompositeReactiveAuthorizationManager(List.of(first, later));

        StepVerifier.create(composite.check(authentication, context))
                .then(() -> {
                    assertThat(calls).containsExactly("first");
                    pending.emit(new AuthorizationDecision(true));
                })
                .assertNext(decision -> assertThat(decision.isGranted()).isTrue())
                .verifyComplete();
        assertThat(calls).containsExactly("first");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void bothConstructorsKeepOrAndIgnoreEmptyBaseDecisions(boolean withConstraints) {
        final List<String> calls = new ArrayList<>();
        final List<ReactiveAuthorizationManager<AuthorizationContext>> managers = List.of(
                (auth, request) -> Mono.empty(),
                (auth, request) -> Mono.just(new AuthorizationDecision(false)),
                (auth, request) -> Mono.just(new AuthorizationDecision(true)),
                (auth, request) -> {
                    calls.add("unreached");
                    return Mono.just(new AuthorizationDecision(true));
                }
        );
        final var composite = withConstraints
                ? new CompositeReactiveAuthorizationManager(managers, List.of())
                : new CompositeReactiveAuthorizationManager(managers);

        StepVerifier.create(composite.check(authentication, context))
                .assertNext(decision -> assertThat(decision.isGranted()).isTrue())
                .verifyComplete();
        assertThat(calls).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void bothConstructorsDenyWithoutBaseManagers(boolean withConstraints) {
        final var composite = withConstraints
                ? new CompositeReactiveAuthorizationManager(List.of(), List.of())
                : new CompositeReactiveAuthorizationManager(List.of());

        StepVerifier.create(composite.check(authentication, context))
                .assertNext(decision -> assertThat(decision.isGranted()).isFalse())
                .verifyComplete();
    }

    @Test
    void constraintsWaitForBaseAndEachOtherBeforeGranting() {
        final var base = TestPublisher.<AuthorizationDecision>create();
        final var first = TestPublisher.<AuthorizationDecision>create();
        final List<String> calls = new ArrayList<>();
        final var composite = new CompositeReactiveAuthorizationManager(
                List.of((auth, request) -> base.mono()),
                List.of(constraint("first", first.mono(), calls), constraint("second", allow(), calls))
        );

        StepVerifier.create(composite.check(authentication, context))
                .then(() -> {
                    assertThat(calls).isEmpty();
                    base.emit(new AuthorizationDecision(true));
                    assertThat(calls).containsExactly("first");
                    first.emit(new AuthorizationDecision(true));
                })
                .assertNext(decision -> assertThat(decision.isGranted()).isTrue())
                .verifyComplete();
        assertThat(calls).containsExactly("first", "second");
    }

    @ParameterizedTest
    @ValueSource(strings = {"deny", "empty", "null"})
    void nonGrantingConstraintDeniesAndStopsWithoutResumingOr(String outcome) {
        final Mono<AuthorizationDecision> denial = switch (outcome) {
            case "deny" -> Mono.just(new AuthorizationDecision(false));
            case "empty" -> Mono.empty();
            default -> null;
        };
        final List<String> calls = new ArrayList<>();
        final var composite = new CompositeReactiveAuthorizationManager(
                List.of((auth, request) -> allow(), (auth, request) -> {
                    calls.add("unreachedBase");
                    return allow();
                }),
                List.of(
                        constraint("first", allow(), calls),
                        constraint("denial", denial, calls),
                        constraint("unreachedConstraint", allow(), calls)
                )
        );

        StepVerifier.create(composite.check(authentication, context))
                .assertNext(decision -> assertThat(decision.isGranted()).isFalse())
                .verifyComplete();
        assertThat(calls).containsExactly("first", "denial");
    }

    @Test
    void waitsForConstraintDenialAndNeverInvokesLaterConstraint() {
        final var pending = TestPublisher.<AuthorizationDecision>create();
        final List<String> calls = new ArrayList<>();
        final var composite = new CompositeReactiveAuthorizationManager(
                List.of((auth, request) -> allow()),
                List.of(constraint("pending", pending.mono(), calls), constraint("unreached", allow(), calls))
        );

        StepVerifier.create(composite.check(authentication, context))
                .then(() -> {
                    assertThat(calls).containsExactly("pending");
                    pending.emit(new AuthorizationDecision(false));
                })
                .assertNext(decision -> assertThat(decision.isGranted()).isFalse())
                .verifyComplete();
        assertThat(calls).containsExactly("pending");
    }

    @ParameterizedTest
    @ValueSource(strings = {"deny", "empty", "null", "noManagers"})
    void baseWithoutGrantDoesNotEvaluateConstraints(String outcome) {
        final Mono<AuthorizationDecision> denial = switch (outcome) {
            case "deny" -> Mono.just(new AuthorizationDecision(false));
            case "empty" -> Mono.empty();
            default -> null;
        };
        final List<String> calls = new ArrayList<>();
        final List<ReactiveAuthorizationManager<AuthorizationContext>> managers = outcome.equals("noManagers")
                ? List.of() : List.of((auth, request) -> denial);
        final var composite = new CompositeReactiveAuthorizationManager(
                managers, List.of(constraint("unreached", allow(), calls))
        );

        StepVerifier.create(composite.check(authentication, context))
                .assertNext(decision -> assertThat(decision.isGranted()).isFalse())
                .verifyComplete();
        assertThat(calls).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void baseExceptionPropagatesWithoutEvaluatingLaterManagersOrConstraints(boolean synchronous) {
        final var failure = new IllegalStateException("base failure");
        final List<String> calls = new ArrayList<>();
        final ReactiveAuthorizationManager<AuthorizationContext> broken = (auth, request) -> {
            if (synchronous) {
                throw failure;
            }
            return Mono.error(failure);
        };
        final var composite = new CompositeReactiveAuthorizationManager(
                List.of(broken, (auth, request) -> {
                    calls.add("unreachedBase");
                    return allow();
                }),
                List.of(constraint("unreachedConstraint", allow(), calls))
        );

        StepVerifier.create(composite.check(authentication, context))
                .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure))
                .verify();
        assertThat(calls).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void constraintExceptionPropagatesAndStops(boolean synchronous) {
        final var failure = new IllegalStateException("constraint failure");
        final List<String> calls = new ArrayList<>();
        final ReactiveAuthorizationConstraint<AuthorizationContext> broken = (auth, request) -> {
            calls.add("broken");
            if (synchronous) {
                throw failure;
            }
            return Mono.error(failure);
        };
        final var composite = new CompositeReactiveAuthorizationManager(
                List.of((auth, request) -> allow()),
                List.of(broken, constraint("unreached", allow(), calls))
        );

        StepVerifier.create(composite.check(authentication, context))
                .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure))
                .verify();
        assertThat(calls).containsExactly("broken");
    }

    private Mono<AuthorizationDecision> allow() {
        return Mono.just(new AuthorizationDecision(true));
    }

    private ReactiveAuthorizationConstraint<AuthorizationContext> constraint(
            String name, Mono<AuthorizationDecision> decision, List<String> calls
    ) {
        return (auth, request) -> {
            assertThat(auth).isSameAs(authentication);
            assertThat(request).isSameAs(context);
            calls.add(name);
            return decision;
        };
    }
}
