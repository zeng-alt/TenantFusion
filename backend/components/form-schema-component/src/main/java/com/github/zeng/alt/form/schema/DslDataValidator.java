package com.github.zeng.alt.form.schema;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 服务端动态表单数据校验器。
 * <p>
 * 按已发布表单模板的 FormDefinition（DSL）校验提交的字段值：
 * <ul>
 *   <li>递归遍历字段节点，收集每个字段的 {@link DslValidationRule 校验规则}</li>
 *   <li>先用已提交数据求值 {@code visibleIf} 条件表达式，隐藏字段跳过校验（与前端渲染语义一致）</li>
 *   <li>规则语义与 FormKit 前端校验对齐（空值仅 {@code required} 生效，除非规则带 {@code empty} 标记）</li>
 * </ul>
 * 返回 {@code 字段名 → 错误文案} 的错误表；合法时为空表。
 *
 * @author zengAlt
 */
public class DslDataValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern URL_PATTERN = Pattern.compile("^(https?|ftp)://[^\\s/$.?#].[^\\s]*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALPHA_PATTERN = Pattern.compile("^[a-zA-Z]+$");
    private static final Pattern ALPHA_DASH_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final Pattern ALPHA_NUM_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");
    private static final Pattern ALPHA_SPACES_PATTERN = Pattern.compile("^[a-zA-Z\\s]+$");
    private static final Pattern ALPHA_NUM_SPACES_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s]+$");

    private final DslParser dslParser;
    private final DslExprEvaluator exprEvaluator;

    public DslDataValidator(DslParser dslParser, DslExprEvaluator exprEvaluator) {
        this.dslParser = dslParser;
        this.exprEvaluator = exprEvaluator;
    }

    /**
     * 校验表单数据
     *
     * @param definition 已发布版本的 FormDefinition JSON
     * @param data       提交的字段值（字段名 → 值）
     * @return 字段名 → 错误文案；合法时为空表
     */
    public Map<String, String> validate(JsonNode definition, Map<String, Object> data) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (definition == null) {
            return errors;
        }
        DslNode root = dslParser.parse(definition);
        if (root == null) {
            return errors;
        }
        validateNode(root, data, errors);
        return errors;
    }

    private void validateNode(DslNode node, Map<String, Object> data, Map<String, String> errors) {
        if (node == null) {
            return;
        }
        if (node.isField()) {
            validateField(node, data, errors);
        }
        if (node.hasChildren()) {
            for (DslNode child : node.children()) {
                validateNode(child, data, errors);
            }
        }
    }

    private void validateField(DslNode node, Map<String, Object> data, Map<String, String> errors) {
        String name = node.name();
        if (name == null || name.isBlank()) {
            return;
        }
        List<DslValidationRule> rules = node.validation();
        if (rules == null || rules.isEmpty()) {
            return;
        }
        // 条件显示：按当前数据求值 visibleIf，隐藏字段不参与校验
        if (!isVisible(node, data)) {
            return;
        }
        // 非阻塞规则（? 修饰符）：命中则整字段跳过
        for (DslValidationRule rule : rules) {
            if (rule.isOptional()) {
                return;
            }
        }
        Object value = data.get(name);
        for (DslValidationRule rule : rules) {
            // 空值语义：非 required 规则在值为空时跳过（除非规则带 empty 标记）
            if (isEmptyValue(value) && !isRequired(rule.rule()) && !rule.isEmptyEnabled()) {
                continue;
            }
            String message = checkRule(rule, value, data);
            if (message != null) {
                errors.put(name, message);
                return;
            }
        }
    }

    private boolean isVisible(DslNode node, Map<String, Object> data) {
        JsonNode visibleIf = node.visibleIf();
        if (visibleIf == null || visibleIf.isNull()) {
            return true;
        }
        return truthy(exprEvaluator.eval(visibleIf, data));
    }

    private String checkRule(DslValidationRule rule, Object value, Map<String, Object> data) {
        String ruleName = rule.rule();
        List<Object> args = rule.args();
        switch (ruleName) {
            case "required":
                return isEmptyValue(value) ? message(rule, "该项为必填项") : null;
            case "number":
                return !isNumeric(value) ? message(rule, "请输入数字") : null;
            case "integer": {
                Double num = toDouble(value);
                return num == null || Math.rint(num) != num ? message(rule, "请输入整数") : null;
            }
            case "email":
                return !(value instanceof String s && EMAIL_PATTERN.matcher(s).matches())
                        ? message(rule, "请输入有效的邮箱地址") : null;
            case "url":
                return !(value instanceof String s && URL_PATTERN.matcher(s).matches())
                        ? message(rule, "请输入有效的链接地址") : null;
            case "min": {
                double arg = doubleArg(args, 0);
                Double num = toDouble(value);
                if (num != null) {
                    return num < arg ? message(rule, "不能小于 " + formatNum(arg)) : null;
                }
                if (value instanceof String s) {
                    return s.length() < arg ? message(rule, "长度不能小于 " + formatNum(arg)) : null;
                }
                return message(rule, "数值过小");
            }
            case "max": {
                double arg = doubleArg(args, 0);
                Double num = toDouble(value);
                if (num != null) {
                    return num > arg ? message(rule, "不能大于 " + formatNum(arg)) : null;
                }
                if (value instanceof String s) {
                    return s.length() > arg ? message(rule, "长度不能大于 " + formatNum(arg)) : null;
                }
                return message(rule, "数值过大");
            }
            case "length": {
                int arg = intArg(args, 0);
                int len = lengthOf(value);
                return len != arg ? message(rule, "长度必须为 " + arg) : null;
            }
            case "min_length": {
                int arg = intArg(args, 0);
                return lengthOf(value) < arg ? message(rule, "长度不能小于 " + arg) : null;
            }
            case "max_length": {
                int arg = intArg(args, 0);
                return lengthOf(value) > arg ? message(rule, "长度不能大于 " + arg) : null;
            }
            case "between": {
                double min = doubleArg(args, 0);
                double max = doubleArg(args, 1);
                Double num = toDouble(value);
                if (num != null) {
                    return num < min || num > max
                            ? message(rule, "数值需介于 " + formatNum(min) + " 与 " + formatNum(max) + " 之间") : null;
                }
                int len = lengthOf(value);
                return len < min || len > max
                        ? message(rule, "长度需介于 " + formatNum(min) + " 与 " + formatNum(max) + " 之间") : null;
            }
            case "pattern":
            case "matches": {
                String pattern = stringArg(args, 0);
                if (pattern == null || pattern.isBlank()) {
                    return null;
                }
                String text = String.valueOf(value);
                try {
                    return Pattern.compile(pattern).matcher(text).matches()
                            ? null : message(rule, "格式不正确");
                } catch (PatternSyntaxException e) {
                    return null;
                }
            }
            case "one_of":
                for (Object allowed : args) {
                    if (valueEquals(value, allowed)) {
                        return null;
                    }
                }
                return message(rule, "不在允许的选项范围内");
            case "not_one_of":
                for (Object forbidden : args) {
                    if (valueEquals(value, forbidden)) {
                        return message(rule, "不允许该值");
                    }
                }
                return null;
            case "alpha":
                return matchesText(value, ALPHA_PATTERN, rule, "只能包含字母");
            case "alpha_dash":
                return matchesText(value, ALPHA_DASH_PATTERN, rule, "只能包含字母、数字、下划线和中划线");
            case "alpha_num":
                return matchesText(value, ALPHA_NUM_PATTERN, rule, "只能包含字母和数字");
            case "alpha_spaces":
                return matchesText(value, ALPHA_SPACES_PATTERN, rule, "只能包含字母和空格");
            case "alpha_num_spaces":
                return matchesText(value, ALPHA_NUM_SPACES_PATTERN, rule, "只能包含字母、数字和空格");
            case "contains":
                return !String.valueOf(value).contains(stringArg(args, 0))
                        ? message(rule, "必须包含指定内容") : null;
            case "date_after":
            case "date_before": {
                LocalDateTime target = parseDateArg(args, 0);
                if (target == null) {
                    return null;
                }
                LocalDateTime actual = parseDateTime(String.valueOf(value));
                if (actual == null) {
                    return message(rule, "日期格式不正确");
                }
                boolean ok = "date_after".equals(ruleName) ? actual.isAfter(target) : actual.isBefore(target);
                return ok ? null : message(rule, "date_after".equals(ruleName) ? "日期需晚于指定日期" : "日期需早于指定日期");
            }
            case "confirmed": {
                String other = stringArg(args, 0);
                Object otherValue = other == null ? null : data.get(other);
                return Objects.equals(String.valueOf(value), String.valueOf(otherValue))
                        ? null : message(rule, "两次输入不一致");
            }
            case "accepted":
                return Boolean.TRUE.equals(value) ? null : message(rule, "请勾选确认");
            default:
                // 未知 / 自定义规则：服务端不拦截（前端负责）
                return null;
        }
    }

    private String matchesText(Object value, Pattern pattern, DslValidationRule rule, String fallback) {
        if (value instanceof String s) {
            return pattern.matcher(s).matches() ? null : message(rule, fallback);
        }
        return message(rule, fallback);
    }

    private String message(DslValidationRule rule, String fallback) {
        return rule.message() != null && !rule.message().isBlank() ? rule.message() : fallback;
    }

    private static boolean isRequired(String ruleName) {
        return "required".equals(ruleName);
    }

    private static boolean isEmptyValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String s) {
            return s.isEmpty();
        }
        if (value instanceof List<?> list) {
            return list.isEmpty();
        }
        if (value instanceof Map<?, ?> map) {
            return map.isEmpty();
        }
        return false;
    }

    private static int lengthOf(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof String s) {
            return s.length();
        }
        if (value instanceof List<?> list) {
            return list.size();
        }
        return String.valueOf(value).length();
    }

    private static Double toDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value instanceof Boolean b) {
            return b ? 1d : 0d;
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static boolean isNumeric(Object value) {
        return toDouble(value) != null;
    }

    private static boolean valueEquals(Object a, Object b) {
        Double na = toDouble(a);
        Double nb = toDouble(b);
        if (na != null && nb != null) {
            return Double.compare(na, nb) == 0;
        }
        return String.valueOf(a).equals(String.valueOf(b));
    }

    private static double doubleArg(List<Object> args, int index) {
        if (index < args.size()) {
            Object arg = args.get(index);
            if (arg instanceof Number n) {
                return n.doubleValue();
            }
            if (arg instanceof String s) {
                try {
                    return Double.parseDouble(s.trim());
                } catch (NumberFormatException ignored) {
                    return 0;
                }
            }
        }
        return 0;
    }

    private static int intArg(List<Object> args, int index) {
        return (int) Math.rint(doubleArg(args, index));
    }

    private static String stringArg(List<Object> args, int index) {
        if (index < args.size() && args.get(index) != null) {
            return String.valueOf(args.get(index));
        }
        return null;
    }

    private static String formatNum(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    private static boolean truthy(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.doubleValue() != 0 && !Double.isNaN(n.doubleValue());
        }
        if (value instanceof String s) {
            return !s.isEmpty();
        }
        return value != null;
    }

    private static LocalDateTime parseDateArg(List<Object> args, int index) {
        String text = stringArg(args, index);
        return text == null ? null : parseDateTime(text);
    }

    private static LocalDateTime parseDateTime(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(text);
            } catch (DateTimeParseException ignored2) {
                return null;
            }
        }
    }
}
