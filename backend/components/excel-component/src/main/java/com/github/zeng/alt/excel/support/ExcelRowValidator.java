package com.github.zeng.alt.excel.support;

import com.github.zeng.alt.excel.read.ExcelViolation;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 逐行 Bean Validation。
 * <p>
 * 旧实现是个 {@code BeanFactoryPostProcessor}，在回调里 {@code getBean(Validator.class)}
 * 塞进 static 字段，然后全靠静态方法调用——违反「只用构造器注入、禁止 getBean()」，
 * 且在没有 {@code Validator} 的应用里会直接启动失败。现在改成普通 bean，
 * 由 {@code ExcelAutoConfiguration} 在 {@code Validator} 存在时才装配。
 * <p>
 * 返回逐字段的 {@link ExcelViolation} 而不是拼好的一段字符串：前端要按字段高亮
 * 单元格、按约束类型分类提示，就必须拿到结构化的失败详情。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public class ExcelRowValidator {

    private final Validator validator;

    public ExcelRowValidator(Validator validator) {
        this.validator = validator;
    }

    /**
     * 校验一行，返回逐字段的失败详情。
     * <p>
     * 结果按字段名排序，保证同一行的错误在多次读取间顺序稳定——否则前端表格
     * 每次刷新顺序都在跳。
     *
     * @param row    数据行
     * @param groups 校验分组，空表示默认分组
     * @param <T>    行类型
     * @return 失败详情；全部通过时为空列表
     */
    public <T> List<ExcelViolation> validate(T row, Class<?>... groups) {
        if (row == null) {
            return List.of(new ExcelViolation("", "数据行为空", "", "NotNull"));
        }
        Set<ConstraintViolation<T>> violations = validator.validate(row, groups);
        if (violations.isEmpty()) {
            return List.of();
        }
        List<ExcelViolation> result = new ArrayList<>(violations.size());
        for (ConstraintViolation<T> violation : violations) {
            result.add(toViolation(violation));
        }
        result.sort(Comparator.comparing(ExcelViolation::field).thenComparing(ExcelViolation::code));
        return List.copyOf(result);
    }

    private static ExcelViolation toViolation(ConstraintViolation<?> violation) {
        return new ExcelViolation(
                violation.getPropertyPath() == null ? "" : violation.getPropertyPath().toString(),
                violation.getMessage(),
                stringify(violation.getInvalidValue()),
                codeOf(violation));
    }

    /**
     * 约束注解的简单名，例如 {@code NotBlank}、{@code Min}，作为前端的分类依据。
     */
    private static String codeOf(ConstraintViolation<?> violation) {
        if (violation.getConstraintDescriptor() == null
                || violation.getConstraintDescriptor().getAnnotation() == null) {
            return "Constraint";
        }
        return violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
    }

    private static String stringify(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
