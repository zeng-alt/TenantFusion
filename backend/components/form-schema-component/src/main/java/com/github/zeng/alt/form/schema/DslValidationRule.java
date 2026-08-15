package com.github.zeng.alt.form.schema;

import java.util.List;

/**
 * 结构化校验规则（与前端 {@code src/types/dsl.ts} 的 {@code ValidationRule} 对齐）。
 *
 * @param rule     规则名（required / min / max / pattern / email / number / integer / one_of 等）
 * @param args     规则参数（如 min:2 的 2，pattern 的正则等）
 * @param message  自定义错误文案（缺省使用内置文案）
 * @param debounce 防抖毫秒数（仅前端生效，服务端忽略）
 * @param empty    空值也执行（对应 FormKit 的 {@code +} 修饰符）
 * @param force    前置规则失败也执行（对应 {@code *} 修饰符，服务端收集全部错误时天然满足）
 * @param optional 非阻塞（对应 {@code ?} 修饰符，命中则整字段跳过校验）
 * @author zengAlt
 */
public record DslValidationRule(
        String rule,
        List<Object> args,
        String message,
        Integer debounce,
        Boolean empty,
        Boolean force,
        Boolean optional
) {

    /** 是否非阻塞（表单仍可提交） */
    public boolean isOptional() {
        return Boolean.TRUE.equals(optional);
    }

    /** 是否空值也执行 */
    public boolean isEmptyEnabled() {
        return Boolean.TRUE.equals(empty);
    }
}
