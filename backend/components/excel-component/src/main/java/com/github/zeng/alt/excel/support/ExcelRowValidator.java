package com.github.zeng.alt.excel.support;

import io.vavr.control.Option;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 逐行 Bean Validation。
 * <p>
 * 旧实现是个 {@code BeanFactoryPostProcessor}，在回调里 {@code getBean(Validator.class)}
 * 塞进 static 字段，然后全靠静态方法调用——违反「只用构造器注入、禁止 getBean()」，
 * 且在没有 {@code Validator} 的应用里会直接启动失败。现在改成普通 bean，
 * 由 {@code ExcelAutoConfiguration} 在 {@code Validator} 存在时才装配。
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
     * 校验一行。
     *
     * @param row    数据行
     * @param groups 校验分组
     * @param <T>    行类型
     * @return 通过时为 {@code none}，否则为拼接好的失败原因
     */
    public <T> Option<String> validate(T row, Class<?>... groups) {
        if (row == null) {
            return Option.of("数据行为空");
        }
        Set<ConstraintViolation<T>> violations = validator.validate(row, groups);
        if (violations.isEmpty()) {
            return Option.none();
        }
        return Option.of(violations.stream()
                .map(ExcelRowValidator::describe)
                .collect(Collectors.joining("; ")));
    }

    private static String describe(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath() == null ? "" : violation.getPropertyPath().toString();
        return path.isEmpty() ? violation.getMessage() : path + " " + violation.getMessage();
    }
}
