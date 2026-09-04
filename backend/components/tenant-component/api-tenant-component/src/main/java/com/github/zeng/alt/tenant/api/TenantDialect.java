package com.github.zeng.alt.tenant.api;

/**
 * 数据库方言差异——目前只有模式级隔离需要，H2 与 PostgreSQL 的 schema 切换/复位语句不同。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
public interface TenantDialect {

    /** 允许出现在 schema / 表名里的字符，用于防注入的白名单校验 */
    java.util.regex.Pattern SAFE_IDENTIFIER =
            java.util.regex.Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,62}");

    /**
     * 校验标识符可安全拼进 SQL，并原样返回。
     * <p>
     * schema 切换语句刻意用<b>裸标识符</b>而不是 {@link #quoteIdentifier(String)}：
     * 加引号会让标识符变成大小写敏感，而 H2 会把未加引号的名字折叠成大写、
     * PostgreSQL 折叠成小写，于是 {@code CREATE SCHEMA tenant_a} 建出的 schema
     * 用 {@code "tenant_a"} 反而找不到。裸标识符 + 白名单校验既避免注入，
     * 又让各数据库按自己的规则折叠大小写。
     *
     * @param identifier 待校验标识符
     * @return 原标识符
     * @throws TenantRoutingException 标识符不合法
     */
    default String requireSafeIdentifier(String identifier) {
        if (identifier == null || !SAFE_IDENTIFIER.matcher(identifier).matches()) {
            throw new TenantRoutingException(
                    "非法的数据库标识符：" + identifier + "，只允许字母、数字与下划线且以字母或下划线开头");
        }
        return identifier;
    }

    /**
     * 方言标识，用于日志与配置显式指定。
     *
     * @return 例如 {@code h2} / {@code postgresql}
     */
    String getName();

    /**
     * 建 schema 的语句，供多租户迁移在首次上线新租户时使用。
     * <p>
     * H2 与 PostgreSQL 都支持 {@code CREATE SCHEMA IF NOT EXISTS}，故给出默认实现；
     * 不支持该语法的数据库自行覆盖。
     *
     * @param schema schema 名
     * @return 可直接执行的 SQL
     */
    default String createSchemaSql(String schema) {
        return "CREATE SCHEMA IF NOT EXISTS " + requireSafeIdentifier(schema);
    }

    /**
     * 切换当前会话 schema 的语句。
     *
     * @param schema schema 名，调用方保证非空
     * @return 可直接执行的 SQL
     */
    String schemaSwitchSql(String schema);

    /**
     * 复位当前会话 schema 的语句，归还连接前执行。
     *
     * @return 可直接执行的 SQL
     */
    String schemaResetSql();

    /**
     * 按方言规则给标识符加引号。
     * <p>
     * <b>不要用它拼 schema 切换语句</b>——原因见 {@link #requireSafeIdentifier(String)}。
     * 保留本方法供确实需要保留大小写的场景使用。
     *
     * @param identifier 标识符
     * @return 带引号的标识符
     */
    String quoteIdentifier(String identifier);
}
