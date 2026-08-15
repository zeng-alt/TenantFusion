package com.github.zeng.alt.workflow.exception;

import com.github.zeng.alt.api.exception.BaseException;

import java.util.Map;

/**
 * 动态表单数据校验失败异常。
 * <p>
 * 携带逐字段错误（字段名 → 错误文案），由 {@code FormValidationAdvice} 渲染为 RFC 9457
 * Problem Details，并在 {@code errors} 扩展属性中透出字段级错误。
 *
 * @author zengAlt
 */
public class FormValidationException extends BaseException {

    private final Map<String, String> fieldErrors;

    /**
     * @param message     概要错误信息（聚合首条错误，便于展示）
     * @param fieldErrors 字段名 → 错误文案
     */
    public FormValidationException(String message, Map<String, String> fieldErrors) {
        super(601, "表单校验未通过", message);
        this.fieldErrors = fieldErrors == null ? Map.of() : fieldErrors;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
