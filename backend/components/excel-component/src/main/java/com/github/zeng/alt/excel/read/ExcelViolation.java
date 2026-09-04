package com.github.zeng.alt.excel.read;

/**
 * 单个字段的校验失败，是 {@link ExcelRowError} 的上游原料。
 * <p>
 * 存在的意义是不把一行的多个约束失败拼成一条字符串——前端要按字段高亮单元格，
 * 就必须拿到「哪个字段、什么值、违反了哪个约束」，而不是一段拼好的中文。
 *
 * @param field         字段名（Bean Validation 的属性路径）
 * @param message       约束消息
 * @param rejectedValue 被拒绝的值，转成字符串；{@code null} 值为空串
 * @param code          约束注解的简单名，如 {@code NotBlank}、{@code Min}
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public record ExcelViolation(String field, String message, String rejectedValue, String code) {
}
