package com.github.zeng.alt.form.schema;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * FormKit 动态表单便携表达式 AST 求值器。
 * <p>
 * 与前端 {@code formkit-form-builder} 的 {@code src/dsl/expr-builtins.ts} 保持同名函数与相同语义
 * （数值化、相等比较、字符串拼接等规则逐条对齐），供 {@code visibleIf} 条件求值及后续计算字段使用。
 * 表达式 AST 为 JSON 结构（Java 可直接反序列化）：
 * <pre>
 *   { "type": "literal", "value": ... }
 *   { "type": "field", "name": "fieldName" }
 *   { "type": "call", "fn": "eq", "args": [Expr, Expr] }
 * </pre>
 *
 * @author zengAlt
 */
public class DslExprEvaluator {

    /**
     * 求值表达式
     *
     * @param expr 表达式 AST（JSON 结构，{@code null}/{@code missing} 时返回 null）
     * @param data 表单数据（字段名 → 值）
     * @return 求值结果
     */
    public Object eval(JsonNode expr, Map<String, Object> data) {
        if (expr == null || expr.isNull() || expr.isMissingNode()) {
            return null;
        }
        String type = expr.path("type").asText();
        return switch (type) {
            case "literal" -> toJavaValue(expr.get("value"));
            case "field" -> data.get(expr.path("name").asText());
            case "call" -> evalCall(expr, data);
            default -> null;
        };
    }

    private Object evalCall(JsonNode expr, Map<String, Object> data) {
        String fn = expr.path("fn").asText();
        List<Object> args = new ArrayList<>();
        for (JsonNode arg : expr.path("args")) {
            args.add(eval(arg, data));
        }
        return apply(fn, args);
    }

    /**
     * 按内置函数名执行（与前端 {@code expr-builtins.ts} 同名函数清单保持一致）
     */
    private Object apply(String fn, List<Object> args) {
        return switch (fn) {
            case "and" -> args.stream().allMatch(DslExprEvaluator::truthy);
            case "or" -> args.stream().anyMatch(DslExprEvaluator::truthy);
            case "not" -> !truthy(arg(args, 0));
            case "eq" -> equal(arg(args, 0), arg(args, 1));
            case "neq" -> !equal(arg(args, 0), arg(args, 1));
            case "gt" -> compare(arg(args, 0), arg(args, 1)) > 0;
            case "gte" -> compare(arg(args, 0), arg(args, 1)) >= 0;
            case "lt" -> compare(arg(args, 0), arg(args, 1)) < 0;
            case "lte" -> compare(arg(args, 0), arg(args, 1)) <= 0;
            case "contains" -> String.valueOf(arg(args, 0) == null ? "" : arg(args, 0)).contains(strOf(arg(args, 1)));
            case "notContains" -> !String.valueOf(arg(args, 0) == null ? "" : arg(args, 0)).contains(strOf(arg(args, 1)));
            case "empty" -> isEmpty(arg(args, 0));
            case "notEmpty" -> !isEmpty(arg(args, 0));
            case "add" -> add(arg(args, 0), arg(args, 1));
            case "sub" -> toNum(arg(args, 0)) - toNum(arg(args, 1));
            case "mul" -> toNum(arg(args, 0)) * toNum(arg(args, 1));
            case "div" -> toNum(arg(args, 0)) / toNum(arg(args, 1));
            case "mod" -> toNum(arg(args, 0)) % toNum(arg(args, 1));
            case "concat" -> {
                StringBuilder sb = new StringBuilder();
                for (Object a : args) {
                    sb.append(strOf(a));
                }
                yield sb.toString();
            }
            case "lower" -> strOf(arg(args, 0)).toLowerCase();
            case "upper" -> strOf(arg(args, 0)).toUpperCase();
            case "trim" -> strOf(arg(args, 0)).trim();
            case "length" -> strOf(arg(args, 0)).length();
            case "coalesce" -> {
                Object a = arg(args, 0);
                yield a == null ? arg(args, 1) : a;
            }
            case "__raw__" -> arg(args, 0);
            case "if" -> truthy(arg(args, 0)) ? arg(args, 1) : arg(args, 2);
            case "sum" -> {
                double sum = 0;
                for (Object a : args) {
                    sum += toNum(a);
                }
                yield sum;
            }
            case "today" -> LocalDate.now().toString();
            case "uuid" -> UUID.randomUUID().toString();
            default -> null;
        };
    }

    private static Object arg(List<Object> args, int index) {
        return index < args.size() ? args.get(index) : null;
    }

    private static String strOf(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /** 与前端 equal 一致的相等语义：两者可数值化则按数值比较，否则按字符串比较 */
    private static boolean equal(Object a, Object b) {
        if ((a == null) && (b == null)) {
            return true;
        }
        double na = toNum(a);
        double nb = toNum(b);
        if (Double.isFinite(na) && Double.isFinite(nb)) {
            return Double.compare(na, nb) == 0;
        }
        return strOf(a).equals(strOf(b));
    }

    /** 与前端 cmp 一致的比较语义 */
    private static double compare(Object a, Object b) {
        double na = toNum(a);
        double nb = toNum(b);
        if (Double.isFinite(na) && Double.isFinite(nb)) {
            return Double.compare(na, nb);
        }
        return strOf(a).compareTo(strOf(b));
    }

    /** 与前端 toNum 一致的数值化规则 */
    private static double toNum(Object v) {
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        if (v instanceof Boolean b) {
            return b ? 1 : 0;
        }
        if (v == null) {
            return 0;
        }
        if (v instanceof String s) {
            String t = s.trim();
            if (t.isEmpty()) {
                return 0;
            }
            try {
                return Double.parseDouble(t);
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        }
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    /** 与前端 Boolean() 一致的真值判断 */
    private static boolean truthy(Object v) {
        if (v instanceof Boolean b) {
            return b;
        }
        if (v instanceof Number n) {
            return n.doubleValue() != 0 && !Double.isNaN(n.doubleValue());
        }
        if (v instanceof String s) {
            return !s.isEmpty();
        }
        return v != null;
    }

    private static boolean isEmpty(Object v) {
        if (v == null) {
            return true;
        }
        if (v instanceof String s) {
            return s.isEmpty();
        }
        if (v instanceof List<?> list) {
            return list.isEmpty();
        }
        if (v instanceof Map<?, ?> map) {
            return map.isEmpty();
        }
        return false;
    }

    private static Object add(Object a, Object b) {
        if (a instanceof String || b instanceof String) {
            return strOf(a) + strOf(b);
        }
        return toNum(a) + toNum(b);
    }

    /** JsonNode → 普通 Java 值（与 Jackson convertValue 语义一致） */
    private Object toJavaValue(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isNumber()) {
            if (node.isIntegralNumber()) {
                return node.longValue();
            }
            return node.decimalValue();
        }
        if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonNode item : node) {
                list.add(toJavaValue(item));
            }
            return list;
        }
        if (node.isObject()) {
            java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
            node.fields().forEachRemaining(entry -> map.put(entry.getKey(), toJavaValue(entry.getValue())));
            return map;
        }
        return node.asText();
    }

    /** 供外部使用的数值化（例如校验规则） */
    public double toNumber(Object value) {
        return toNum(value);
    }

    /** 供外部使用的数字判断：可转为有限数值 */
    public boolean isNumeric(Object value) {
        return Double.isFinite(toNum(value));
    }

    /** 供外部使用的 BigDecimal 化 */
    public BigDecimal toDecimal(Object value) {
        double num = toNum(value);
        return Double.isFinite(num) ? BigDecimal.valueOf(num) : null;
    }
}
