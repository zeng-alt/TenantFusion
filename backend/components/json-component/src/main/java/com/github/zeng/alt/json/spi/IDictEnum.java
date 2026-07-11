package com.github.zeng.alt.json.spi;

/**
 * 枚举字典翻译接口。实现此接口的枚举可被 {@code @DictFormat} 识别并自动翻译。
 * <p>
 * 匹配规则：优先使用 {@link #getCode()} 与字段值匹配；若 {@link #getCode()} 返回 {@code null}，
 * 则使用 {@link Enum#name()} 匹配。
 *
 * <pre>{@code
 * public enum StatusEnum implements IDictEnum {
 *     ACTIVE("1", "启用"),
 *     INACTIVE("0", "禁用");
 *
 *     private final String code;
 *     private final String label;
 *
 *     StatusEnum(String code, String label) { this.code = code; this.label = label; }
 *     public String getCode() { return code; }
 *     public String getLabel() { return label; }
 * }
 * }</pre>
 */
public interface IDictEnum {

    /** 翻译后的展示文本。 */
    String getLabel();

    /** 存储在数据库 / 字段中的原始编码。返回 {@code null} 时使用 {@link Enum#name()} 作为编码。 */
    default String getCode() {
        return null;
    }

}
