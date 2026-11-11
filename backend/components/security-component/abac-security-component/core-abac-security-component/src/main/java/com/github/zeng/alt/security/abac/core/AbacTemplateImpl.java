package com.github.zeng.alt.security.abac.core;

import com.github.zeng.alt.security.abac.AbacAuthorizationBuilder;
import com.github.zeng.alt.security.abac.AbacContext;
import com.github.zeng.alt.security.abac.AbacFailure;
import com.github.zeng.alt.security.abac.AbacPolicy;
import com.github.zeng.alt.security.abac.AbacTemplate;

import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 所有适用策略按 AND 短路执行，加载/求值失败不会降级为放行。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public final class AbacTemplateImpl implements AbacTemplate {
    private final AbacPolicyLoader policyLoader;
    private final AbacEvaluator evaluator;
    private final AbacProperties properties;

    @Override
    public AbacAuthorizationBuilder authorize(AbacContext context) {
        return new AbacAuthorizationBuilder(required -> Try.of(() -> List.copyOf(policyLoader.list(context)))
                .toEither()
                .mapLeft(ignored -> new AbacFailure("POLICY_LOAD_FAILED", "策略加载失败"))
                .flatMap(policies -> evaluate(policies, context,
                        required || properties.getRequiredPermissions().contains(context.permission()))));
    }

    private Either<AbacFailure, Boolean> evaluate(List<AbacPolicy> policies, AbacContext context, boolean required) {
        if (policies.stream().anyMatch(policy -> !context.tenant().equals(policy.tenant()))) {
            return Either.left(new AbacFailure("TENANT_MISMATCH", "策略租户不匹配"));
        }
        if (policies.stream().anyMatch(policy -> policy.resourceId().isDefined())
                && !context.resource().containsKey("id")) {
            return Either.left(new AbacFailure("RESOURCE_MISSING", "策略要求可信资源 ID"));
        }
        final List<AbacPolicy> applicable = policies.stream()
                .filter(policy -> policy.resourceId().isEmpty()
                        || policy.resourceId().get().equals(context.resource().get("id")))
                .toList();
        if (applicable.isEmpty()) {
            return required || !policies.isEmpty()
                    ? Either.left(new AbacFailure("POLICY_MISSING", "没有适用的授权策略")) : Either.right(true);
        }
        return io.vavr.collection.List.ofAll(applicable).iterator()
                .map(policy -> evaluate(policy, context))
                .find(Either::isLeft)
                .getOrElse(Either.right(true));
    }

    private Either<AbacFailure, Boolean> evaluate(AbacPolicy policy, AbacContext context) {
        if (!policy.preAuth()) {
            return Either.left(new AbacFailure("POST_AUTH_UNSUPPORTED", "暂不支持执行后授权策略"));
        }
        return evaluator.evaluate(policy.condition(), context)
                .flatMap(granted -> granted ? Either.right(true)
                        : Either.left(new AbacFailure("POLICY_DENIED", "不满足资源访问条件")));
    }
}
