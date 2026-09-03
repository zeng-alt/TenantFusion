package com.github.zeng.alt.tenant.api;

/**
 * 租户隔离预设。
 * <p>
 * 隔离能力实际由 {@link TenantRouting} 的四个独立旋钮表达，本枚举只是常用组合的入口：
 * 配置 {@code alt.tenant.mode=SCHEMA} 等价于「只打开 schemaName 这一档」。
 * 需要叠加（例如独立库内再用非默认 schema）时直接配置各旋钮，不必受本枚举限制。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
public enum TenantMode {

    /** 不隔离，所有租户共享同一份数据 */
    NONE,

    /** 行级：同表同库，靠 {@code @TenantId} 判别列区分 */
    ROW,

    /** 表级：同库同 schema，表名带租户后缀 */
    TABLE,

    /** 模式级：同库不同 schema */
    SCHEMA,

    /** 库级：不同数据源 */
    DATABASE;

    /**
     * 宽松解析，兼容旧的 {@code Tenant.Mode#COLUMN} 写法（等价于 {@link #ROW}）。
     *
     * @param text 待解析文本，可为 null
     * @return 解析结果，无法识别时返回 null
     */
    public static TenantMode of(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalized = text.trim();
        if ("COLUMN".equalsIgnoreCase(normalized)) {
            return ROW;
        }
        for (TenantMode mode : values()) {
            if (mode.name().equalsIgnoreCase(normalized)) {
                return mode;
            }
        }
        return null;
    }
}
