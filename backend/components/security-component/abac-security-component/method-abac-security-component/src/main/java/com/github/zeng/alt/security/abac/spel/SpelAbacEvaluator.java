package com.github.zeng.alt.security.abac.spel;

import com.github.zeng.alt.security.abac.AbacContext;
import com.github.zeng.alt.security.abac.AbacFailure;
import com.github.zeng.alt.security.abac.core.AbacEvaluator;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.vavr.control.Either;
import io.vavr.control.Try;
import org.springframework.expression.AccessException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 缓存解析对象，针对每次只读快照独立求值，支持点号、方法变量与已注册授权函数。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class SpelAbacEvaluator implements AbacEvaluator {
    private static final AbacFailure INVALID = new AbacFailure("INVALID_ATTRIBUTES", "策略引用未知属性或不支持的表达式");
    private final SpelExpressionParser parser;
    private final Cache<String, CachedCondition> cache;
    private final Map<String, AbacFunctionResolver> functions;
    private final AbacExpressionHelper expressions = new AbacExpressionHelper();

    public SpelAbacEvaluator() { this(new SpelExpressionParser()); }
    SpelAbacEvaluator(SpelExpressionParser parser) { this(parser, new AbacSpelProperties()); }
    SpelAbacEvaluator(SpelExpressionParser parser, AbacSpelProperties properties) {
        this(parser, properties, List.of());
    }

    public SpelAbacEvaluator(SpelExpressionParser parser, AbacSpelProperties properties,
            List<AbacFunctionResolver> customFunctions) {
        this.parser = parser;
        this.cache = CacheBuilder.newBuilder().maximumSize(properties.getMaximumSize())
                .expireAfterWrite(properties.getCacheTtl()).build();
        final Map<String, AbacFunctionResolver> registered = new LinkedHashMap<>();
        customFunctions.forEach(resolver -> resolver.functions().forEach(name -> registered.putIfAbsent(name, resolver)));
        final AbacFunctionResolver builtIn = new AbacSecurityFunctionResolver();
        builtIn.functions().forEach(name -> registered.putIfAbsent(name, builtIn));
        this.functions = Collections.unmodifiableMap(registered);
    }

    @Override
    public Either<AbacFailure, Boolean> prepare(String condition) {
        return getCondition(condition).flatMap(cached -> cached.valid() ? Either.right(true) : Either.left(INVALID));
    }

    /** 返回经过语法检查的静态引用路径。 */
    public Either<AbacFailure, List<String>> references(String condition) {
        return getCondition(condition).flatMap(cached -> cached.valid()
                ? Either.right(cached.references()) : Either.left(INVALID));
    }

    public Set<String> functions() { return functions.keySet(); }

    @Override
    public Either<AbacFailure, Boolean> evaluate(String condition, AbacContext context) {
        return getCondition(condition).flatMap(cached -> evaluate(cached, context));
    }

    private Either<AbacFailure, CachedCondition> getCondition(String condition) {
        return Try.of(() -> cache.get(condition, () -> {
            final SpelExpression expression = parser.parseRaw(condition);
            final List<String> references = new ArrayList<>();
            expressions.references(expression.getAST(), references);
            return new CachedCondition(expression, expressions.valid(expression.getAST(), functions.keySet()),
                    references.stream().distinct().toList());
        })).toEither(new AbacFailure("INVALID_EXPRESSION", "策略表达式语法错误"));
    }

    private Either<AbacFailure, Boolean> evaluate(CachedCondition cached, AbacContext context) {
        if (!cached.valid()) { return Either.left(INVALID); }
        final Map<String, Object> root = root(context);
        if (cached.references().stream().anyMatch(path -> !exists(path, root, context.invocation().variables()))) {
            return Either.left(INVALID);
        }
        final var evaluation = SimpleEvaluationContext.forPropertyAccessors(new AbacMapAccessor())
                .withMethodResolvers((ctx, target, name, types) -> {
                    if (target != root || !functions.containsKey(name)) { return null; }
                    return (ignored, object, arguments) -> {
                        final var decision = functions.get(name).evaluate(name, Arrays.asList(arguments), context);
                        if (decision.isLeft()) { throw new AccessException(decision.getLeft().message()); }
                        return new TypedValue(decision.get());
                    };
                }, new AbacValueMethodResolver()).build();
        context.invocation().variables().forEach(evaluation::setVariable);
        return Try.of(() -> cached.expression().getValue(evaluation, root)).toEither()
                .mapLeft(failure -> new AbacFailure("EVALUATION_FAILED", failure.getMessage()))
                .flatMap(value -> value instanceof Boolean granted ? Either.right(granted)
                        : Either.left(new AbacFailure("NON_BOOLEAN_RESULT", "策略必须返回布尔值")));
    }

    private Map<String, Object> root(AbacContext context) {
        final Map<String, Object> root = new LinkedHashMap<>();
        root.put("principal", context.principal());
        root.put("resource", context.resource());
        root.put("env", context.env());
        final Map<String, Object> authentication = new LinkedHashMap<>(context.invocation().authentication());
        authentication.put("principal", context.principal());
        root.put("authentication", Collections.unmodifiableMap(authentication));
        root.put("tenant", context.tenant());
        root.put("permission", context.permission());
        root.put("permitAll", true);
        root.put("denyAll", false);
        if (!context.invocation().preAuth()) {
            root.put("returnObject", context.invocation().returnObject().getOrNull());
        }
        return Collections.unmodifiableMap(root);
    }

    private boolean exists(String path, Map<String, Object> root, Map<String, Object> variables) {
        final String[] keys = path.split("\\.");
        Object current = path.startsWith("#") ? variables : root;
        for (int index = 0; index < keys.length; index++) {
            final String key = index == 0 ? keys[index].replaceFirst("^#", "") : keys[index];
            if (index == 0 && (key.equals("root") || key.equals("this"))) { current = root; continue; }
            if (current == null) { return true; }
            if (current instanceof Map<?, ?> map) {
                if (!map.containsKey(key)) { return false; }
                current = map.get(key);
            } else if (current instanceof List<?> list && key.matches("[0-9]+")) {
                final int position = Integer.parseInt(key);
                if (position >= list.size()) { return false; }
                current = list.get(position);
            } else if (current instanceof String && key.equals("length")) {
                return true;
            } else if (current instanceof java.util.Collection<?> && (key.equals("size") || key.equals("empty"))) {
                return true;
            } else { return false; }
        }
        return true;
    }

    private record CachedCondition(SpelExpression expression, boolean valid, List<String> references) { }
}
