package com.github.zeng.alt.tenant.table;

import com.github.zeng.alt.tenant.api.TenantRouting;
import com.github.zeng.alt.tenant.api.TenantSqlRewriter;
import com.github.zeng.alt.tenant.core.TenantProperties;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 给表名追加租户后缀，例如 {@code main_user} → {@code main_user_t001}。
 * <p>
 * <b>已知局限，必须了解后再启用</b>：本重写作用在 Hibernate 生成的 SQL 文本上，
 * 因此以下情形不受支持或可能误伤——
 * <ul>
 *   <li>原生查询（{@code createNativeQuery}）与 {@code @Formula} 里的表名同样会被改写，
 *       但拼接方式若与 Hibernate 不同则未必正确</li>
 *   <li>表名恰好作为字符串字面量出现在 SQL 中（如 {@code where name = 'main_user'}）</li>
 *   <li>Camunda 等自带 SQL 的框架不在本模块覆盖范围内</li>
 * </ul>
 * 为此表级隔离只对显式标注 {@code @TenantScope(table = true)} 的实体开启，且用词边界匹配，
 * 避免 {@code main_user} 命中 {@code main_user_role}。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
public class TableSuffixSqlRewriter implements TenantSqlRewriter {

    private final TenantTableRegistry tableRegistry;
    private final String separator;
    private final Map<String, Pattern> patternCache = new ConcurrentHashMap<>();

    public TableSuffixSqlRewriter(TenantTableRegistry tableRegistry, TenantProperties properties) {
        this.tableRegistry = tableRegistry;
        this.separator = properties.getTableSuffixSeparator() == null
                ? "_"
                : properties.getTableSuffixSeparator();
    }

    @Override
    public boolean supports(TenantRouting routing) {
        return routing.isTableIsolated() && !tableRegistry.tableNames().isEmpty();
    }

    @Override
    public String rewrite(String sql, TenantRouting routing) {
        Set<String> tables = tableRegistry.tableNames();
        if (tables.isEmpty()) {
            return sql;
        }
        String suffix = separator + routing.tableSuffix();
        String result = sql;
        for (String table : tables) {
            Pattern pattern = patternCache.computeIfAbsent(table, TableSuffixSqlRewriter::compile);
            Matcher matcher = pattern.matcher(result);
            if (matcher.find()) {
                // 幂等：已带后缀的不再追加，靠 (?!suffix) 之外再核对一次
                result = matcher.replaceAll(mr -> Matcher.quoteReplacement(mr.group() + suffix));
            }
        }
        return result;
    }

    /**
     * 词边界匹配：{@code (?<![\w."])table(?![\w."])}。
     * <p>前后同时排除引号，避免命中 {@code "main_user"} 这类被引用过的标识符与字符串字面量。
     */
    private static Pattern compile(String table) {
        return Pattern.compile(
                "(?<![\\w.\"'])" + Pattern.quote(table) + "(?![\\w\"'])",
                Pattern.CASE_INSENSITIVE);
    }
}
