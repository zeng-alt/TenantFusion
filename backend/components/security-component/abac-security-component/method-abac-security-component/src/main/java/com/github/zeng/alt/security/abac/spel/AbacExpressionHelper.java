package com.github.zeng.alt.security.abac.spel;

import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.ast.CompoundExpression;
import org.springframework.expression.spel.ast.Indexer;
import org.springframework.expression.spel.ast.Literal;
import org.springframework.expression.spel.ast.MethodReference;
import org.springframework.expression.spel.ast.PropertyOrFieldReference;
import org.springframework.expression.spel.ast.VariableReference;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * 校验数据表达式节点并收集静态属性路径，供运行期和工作台共用。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
final class AbacExpressionHelper {
    private static final Set<String> ROOTS = Set.of("principal", "resource", "env",
            "authentication", "tenant", "permission", "returnObject", "permitAll", "denyAll");
    private static final Set<String> OPERATORS = Set.of("OpAnd", "OpOr", "OperatorNot", "OpEQ", "OpNE",
            "OpGT", "OpGE", "OpLT", "OpLE", "Ternary", "Elvis", "InlineList");

    boolean valid(SpelNode node, Set<String> functions) {
        if (node instanceof PropertyOrFieldReference property) {
            return !Set.of("class", "classLoader", "declaringClass").contains(property.getName());
        }
        if (node instanceof Indexer && (node.getChildCount() != 1 || !(node.getChild(0) instanceof Literal))) {
            return false;
        }
        if (node instanceof MethodReference method && !functions.contains(method.getName())
                && !AbacValueMethodResolver.METHODS.contains(method.getName())) {
            return false;
        }
        if (!(node instanceof Literal || node instanceof VariableReference || node instanceof CompoundExpression
                || node instanceof Indexer || node instanceof MethodReference
                || OPERATORS.contains(node.getClass().getSimpleName()))) {
            return false;
        }
        if (node instanceof CompoundExpression compound
                && compound.getChild(0) instanceof PropertyOrFieldReference root && !ROOTS.contains(root.getName())) {
            return false;
        }
        return IntStream.range(0, node.getChildCount()).allMatch(index -> valid(node.getChild(index), functions));
    }

    void references(SpelNode node, List<String> references) {
        if (node instanceof CompoundExpression) {
            final StringBuilder path = new StringBuilder();
            for (int index = 0; index < node.getChildCount(); index++) {
                final SpelNode child = node.getChild(index);
                if (child instanceof VariableReference || child instanceof PropertyOrFieldReference) {
                    if (!path.isEmpty()) { path.append('.'); }
                    path.append(child.toStringAST().replace("?.", ""));
                } else if (child instanceof Indexer && child.getChild(0) instanceof Literal literal) {
                    path.append('.').append(literal.getLiteralValue().getValue());
                } else {
                    references(child, references);
                }
            }
            if (!path.isEmpty()) { references.add(path.toString()); }
            return;
        }
        if (node instanceof VariableReference || node instanceof PropertyOrFieldReference) {
            references.add(node.toStringAST());
        }
        IntStream.range(0, node.getChildCount()).forEach(index -> references(node.getChild(index), references));
    }
}
