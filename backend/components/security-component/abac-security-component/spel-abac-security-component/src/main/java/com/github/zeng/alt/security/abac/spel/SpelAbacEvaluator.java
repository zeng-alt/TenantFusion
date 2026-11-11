package com.github.zeng.alt.security.abac.spel;

import com.github.zeng.alt.security.abac.AbacContext;
import com.github.zeng.alt.security.abac.AbacFailure;
import com.github.zeng.alt.security.abac.core.AbacEvaluator;

import io.vavr.control.Either;
import io.vavr.control.Try;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.ast.BooleanLiteral;
import org.springframework.expression.spel.ast.CompoundExpression;
import org.springframework.expression.spel.ast.Indexer;
import org.springframework.expression.spel.ast.IntLiteral;
import org.springframework.expression.spel.ast.LongLiteral;
import org.springframework.expression.spel.ast.NullLiteral;
import org.springframework.expression.spel.ast.OpAnd;
import org.springframework.expression.spel.ast.OpEQ;
import org.springframework.expression.spel.ast.OpGE;
import org.springframework.expression.spel.ast.OpGT;
import org.springframework.expression.spel.ast.OpLE;
import org.springframework.expression.spel.ast.OpLT;
import org.springframework.expression.spel.ast.OpNE;
import org.springframework.expression.spel.ast.OpOr;
import org.springframework.expression.spel.ast.OperatorNot;
import org.springframework.expression.spel.ast.PropertyOrFieldReference;
import org.springframework.expression.spel.ast.RealLiteral;
import org.springframework.expression.spel.ast.StringLiteral;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

/**
 * 只允许标量比较、布尔运算及全局根变量 subject/resource/environment['key']、tenant、permission 访问。
 * 全局变量以根对象属性暴露（无 #）；方法参数 # 变量不向不可信策略开放。
 * 解析结果与结构白名单校验按条件字符串缓存；属性存在性依赖运行时上下文，每次求值重新校验，
 * 避免两个缺失属性因 null == null 被误判为许可。解析失败不缓存。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class SpelAbacEvaluator implements AbacEvaluator {
    private static final Set<Class<?>> ALLOWED_NODES = Set.of(
            BooleanLiteral.class, IntLiteral.class, LongLiteral.class, RealLiteral.class,
            StringLiteral.class, NullLiteral.class, OpAnd.class, OpOr.class, OperatorNot.class,
            OpEQ.class, OpNE.class, OpGT.class, OpGE.class, OpLT.class, OpLE.class
    );
    private static final Set<String> MAP_GLOBALS = Set.of("subject", "resource", "environment");
    private static final Set<String> SCALAR_GLOBALS = Set.of("tenant", "permission");
    private static final AbacFailure INVALID_ATTRIBUTES =
            new AbacFailure("INVALID_ATTRIBUTES", "策略引用缺失属性或不支持的表达式");

    private final SpelExpressionParser parser;
    private final Map<String, CachedCondition> cache = new ConcurrentHashMap<>();

    public SpelAbacEvaluator() {
        this(new SpelExpressionParser());
    }

    SpelAbacEvaluator(SpelExpressionParser parser) {
        this.parser = parser;
    }

    @Override
    public Either<AbacFailure, Boolean> evaluate(String condition, AbacContext context) {
        return Try.of(() -> cache.computeIfAbsent(condition, this::parse))
                .toEither(new AbacFailure("INVALID_EXPRESSION", "策略表达式无效"))
                .flatMap(cached -> evaluate(cached, context));
    }

    private Either<AbacFailure, Boolean> evaluate(CachedCondition cached, AbacContext context) {
        if (!cached.structurallyValid()) {
            return Either.left(INVALID_ATTRIBUTES);
        }
        final Map<String, Map<String, Object>> attributes = Map.of(
                "subject", context.subject(), "resource", context.resource(), "environment", context.environment());
        if (cached.references().stream().anyMatch(ref -> !attributes.get(ref.namespace()).containsKey(ref.key()))) {
            return Either.left(INVALID_ATTRIBUTES);
        }
        final Map<String, Object> root = Map.of(
                "subject", context.subject(), "resource", context.resource(), "environment", context.environment(),
                "tenant", context.tenant(), "permission", context.permission());
        final var evaluation = SimpleEvaluationContext.forPropertyAccessors(new MapAccessor()).build();
        return Try.of(() -> cached.expression().getValue(evaluation, root)).toEither()
                .mapLeft(ignored -> new AbacFailure("EVALUATION_FAILED", "策略表达式求值失败"))
                .flatMap(value -> value instanceof Boolean granted ? Either.right(granted)
                        : Either.left(new AbacFailure("NON_BOOLEAN_RESULT", "策略必须返回布尔值")));
    }

    private CachedCondition parse(String condition) {
        final SpelExpression expression = parser.parseRaw(condition);
        final List<AttributeReference> references = new ArrayList<>();
        final boolean structurallyValid = valid(expression.getAST(), references);
        return new CachedCondition(expression, structurallyValid, List.copyOf(references));
    }

    private boolean valid(SpelNode node, List<AttributeReference> references) {
        if (node instanceof CompoundExpression) {
            return validAttribute(node, references);
        }
        if (node instanceof PropertyOrFieldReference reference) {
            return SCALAR_GLOBALS.contains(reference.getName());
        }
        return ALLOWED_NODES.contains(node.getClass()) && IntStream.range(0, node.getChildCount())
                .allMatch(index -> valid(node.getChild(index), references));
    }

    private boolean validAttribute(SpelNode node, List<AttributeReference> references) {
        if (node.getChildCount() != 2 || !(node.getChild(0) instanceof PropertyOrFieldReference reference)
                || !MAP_GLOBALS.contains(reference.getName())
                || !(node.getChild(1) instanceof Indexer indexer) || indexer.getChildCount() != 1
                || !(indexer.getChild(0) instanceof StringLiteral literal)) {
            return false;
        }
        references.add(new AttributeReference(reference.getName(), (String) literal.getLiteralValue().getValue()));
        return true;
    }

    private record CachedCondition(Expression expression, boolean structurallyValid, List<AttributeReference> references) {
    }

    private record AttributeReference(String namespace, String key) {
    }
}