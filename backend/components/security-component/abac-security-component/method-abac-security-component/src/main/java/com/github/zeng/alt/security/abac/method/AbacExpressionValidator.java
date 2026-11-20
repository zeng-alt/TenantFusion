package com.github.zeng.alt.security.abac.method;

import com.github.zeng.alt.security.abac.AbacFailure;
import com.github.zeng.alt.security.abac.spel.SpelAbacEvaluator;
import io.vavr.control.Either;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 使用字段目录检查表达式引用和执行阶段，不执行被保护的业务方法。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class AbacExpressionValidator {
    private final SpelAbacEvaluator evaluator;

    public AbacExpressionValidator(SpelAbacEvaluator evaluator) { this.evaluator = evaluator; }

    public Either<AbacFailure, Boolean> validate(String condition, AbacSchemaDto schema, boolean preAuth) {
        if (schema.methods().isEmpty()) {
            return Either.left(new AbacFailure("METHOD_NOT_FOUND", "该权限未绑定授权方法"));
        }
        return evaluator.references(condition).flatMap(references -> {
            final List<List<AbacFieldDto>> contexts = new ArrayList<>();
            schema.methods().forEach(method -> {
                final List<AbacFieldDto> fields = new ArrayList<>(schema.globals());
                fields.addAll(method.parameters().stream().map(AbacParameterDto::schema).toList());
                if (!preAuth) { fields.add(method.returned()); }
                contexts.add(fields);
            });
            for (List<AbacFieldDto> fields : contexts) {
                final Set<String> paths = new HashSet<>();
                fields.forEach(field -> flatten(field, paths));
                for (String reference : references) {
                    final String normalized = reference.replace("authentication.principal", "principal")
                            .replaceFirst("^#(?:root|this)\\.", "").replaceAll("\\.[0-9]+(?=\\.|$)", "[0]");
                    if (reference.equals("#root") || reference.equals("#this")) { continue; }
                    if (!paths.contains(normalized)) {
                        return Either.left(new AbacFailure("UNKNOWN_ATTRIBUTE", "未知变量或阶段不支持的字段: " + reference));
                    }
                }
            }
            return Either.right(true);
        });
    }

    private void flatten(AbacFieldDto field, Set<String> paths) {
        paths.add(field.path());
        if (field.type().contains("String")) { paths.add(field.path() + ".length"); }
        if (field.type().contains("List") || field.type().contains("Collection") || field.type().endsWith("[]")) {
            paths.add(field.path() + ".size");
            paths.add(field.path() + ".empty");
            paths.add(field.path() + "[0]");
        }
        field.fields().forEach(child -> flatten(child, paths));
    }
}
