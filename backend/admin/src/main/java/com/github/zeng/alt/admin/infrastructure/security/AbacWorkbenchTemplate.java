package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.query.api.dto.AbacPolicyCmd;
import com.github.zeng.alt.admin.query.api.dto.AbacSimulationCmd;
import com.github.zeng.alt.admin.query.api.dto.AbacSimulationVO;
import com.github.zeng.alt.security.abac.AbacContext;
import com.github.zeng.alt.security.abac.AbacFailure;
import com.github.zeng.alt.security.abac.AbacInvocationDto;
import com.github.zeng.alt.security.abac.AbacPhaseDto;
import com.github.zeng.alt.security.abac.method.AbacExpressionValidator;
import com.github.zeng.alt.security.abac.method.AbacMethodResolver;
import com.github.zeng.alt.security.abac.method.AbacSchemaDto;
import com.github.zeng.alt.security.abac.spel.SpelAbacEvaluator;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 工作台与运行时共用元数据、预解析缓存和求值器，试算不调用业务方法。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Component
@RequiredArgsConstructor
public class AbacWorkbenchTemplate {
    private final AbacMethodResolver methods;
    private final SpelAbacEvaluator evaluator;

    public AbacSchemaDto schema(String permission) {
        return methods.getSchema(permission);
    }

    public Either<AbacFailure, Boolean> validate(AbacPolicyCmd command) {
        return new AbacExpressionValidator(evaluator)
                .validate(command.condition(), schema(command.permission()), command.preAuth());
    }

    public AbacSimulationVO simulate(String tenant, AbacSimulationCmd command) {
        final AbacSchemaDto catalog = schema(command.permission());
        final AbacSchemaDto schema = new AbacSchemaDto(catalog.permission(),
                catalog.globals(),
                catalog.methods().stream().filter(method -> method.method().equals(command.method())).toList(),
                catalog.functions());
        return new AbacExpressionValidator(evaluator).validate(command.condition(), schema, command.preAuth())
                .flatMap(ignored -> Try.of(() -> context(tenant, command, schema)).toEither()
                        .mapLeft(failure -> new AbacFailure("INVALID_CONTEXT", "上下文需要 JSON 对象、数组或标量")))
                .flatMap(context -> evaluator.evaluate(command.condition(), context))
                .fold(failure -> new AbacSimulationVO(false, failure.code(), failure.message()),
                        granted -> new AbacSimulationVO(granted, granted ? "ALLOWED" : "POLICY_DENIED",
                                granted ? "表达式允许" : "表达式拒绝"));
    }

    private AbacContext context(String tenant, AbacSimulationCmd command, AbacSchemaDto schema) {
        final Map<String, Object> sample = command.context();
        final boolean transactional = schema.methods().stream().allMatch(method -> method.transactional());
        final AbacInvocationDto invocation = new AbacInvocationDto(
                new AbacPhaseDto(command.preAuth(), transactional), object(sample, "authentication"),
                variables(sample, schema), command.preAuth() ? Option.none() : Option.of(sample.get("returnObject")));
        return new AbacContext(tenant, command.permission(), object(sample, "principal"), object(sample, "resource"),
                object(sample, "env")).withInvocation(invocation);
    }

    private Map<String, Object> variables(Map<String, Object> sample, AbacSchemaDto schema) {
        final Map<String, Object> supplied = object(sample, "variables");
        final Map<String, Object> variables = new java.util.LinkedHashMap<>(supplied);
        if (!schema.methods().isEmpty()) {
            final var method = schema.methods().getFirst();
            method.parameters().stream().filter(parameter -> !parameter.name().matches("[pa][0-9]+"))
                    .filter(parameter -> supplied.containsKey(parameter.name())).forEach(parameter -> {
                        variables.put("p" + parameter.index(), supplied.get(parameter.name()));
                        variables.put("a" + parameter.index(), supplied.get(parameter.name()));
                    });
        }
        return variables;
    }

    private Map<String, Object> object(Map<String, Object> sample, String name) {
        final Object value = sample.getOrDefault(name, Map.of());
        if (!(value instanceof Map<?, ?> map) || map.keySet().stream().anyMatch(key -> !(key instanceof String))) {
            throw new IllegalArgumentException("上下文分组必须是对象");
        }
        @SuppressWarnings("unchecked") final Map<String, Object> values = (Map<String, Object>) map;
        return values;
    }
}
