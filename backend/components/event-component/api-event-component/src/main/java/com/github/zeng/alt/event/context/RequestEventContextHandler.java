package com.github.zeng.alt.event.context;

import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import com.github.zeng.alt.tenant.api.TenantRouting;
import io.vavr.Tuple;
import io.vavr.collection.HashMap;
import io.vavr.control.Option;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 保存并恢复租户路由与用户身份；不保存凭据或授予角色权限。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public class RequestEventContextHandler implements EventContextHandler {

    @Override
    public String key() {
        return "request";
    }

    @Override
    public Map<String, String> capture() {
        final var routing = Option.of(TenantContextHolder.getRouting());
        return Map.copyOf(HashMap.ofEntries(
                Tuple.of("tenantId", TenantContextHolder.getTenantId()),
                Tuple.of("database", TenantContextHolder.getDatabase()),
                Tuple.of("schema", TenantContextHolder.getSchema()),
                Tuple.of("routing.tenantId", routing.map(TenantRouting::tenantId).getOrNull()),
                Tuple.of("routing.dataSourceKey", routing.map(TenantRouting::dataSourceKey).getOrNull()),
                Tuple.of("routing.schemaName", routing.map(TenantRouting::schemaName).getOrNull()),
                Tuple.of("routing.tableSuffix", routing.map(TenantRouting::tableSuffix).getOrNull()),
                Tuple.of("routing.rowIsolated", routing.map(r -> Boolean.toString(r.rowIsolated())).getOrNull()),
                Tuple.of("userId", UserContextHolder.getId()),
                Tuple.of("username", UserContextHolder.getUsername())
        ).filter((key, value) -> value != null).toJavaMap());
    }

    @Override
    public Scope restore(Map<String, String> contextByKey) {
        final Map<String, String> previousTenantByKey = capture();
        final SecurityContext previousSecurityContext = SecurityContextHolder.getContext();
        final SecurityContext restoredSecurityContext = createSecurityContext(contextByKey);
        applyTenant(contextByKey);
        SecurityContextHolder.setContext(restoredSecurityContext);
        return () -> {
            applyTenant(previousTenantByKey);
            SecurityContextHolder.setContext(previousSecurityContext);
        };
    }

    private static SecurityContext createSecurityContext(Map<String, String> contextByKey) {
        final SecurityContext context = SecurityContextHolder.createEmptyContext();
        Option.of(contextByKey.get("userId")).filter(StringUtils::hasText)
                .flatMap(id -> Option.of(contextByKey.get("username")).filter(StringUtils::hasText)
                        .map(username -> SecurityUser.withUsername(username)
                                .id(id)
                                .tenant(contextByKey.get("tenantId"))
                                .database(contextByKey.get("database"))
                                .schema(contextByKey.get("schema"))
                                .build()))
                .peek(user -> context.setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(user, null, List.of())
                ));
        return context;
    }

    private static void applyTenant(Map<String, String> contextByKey) {
        final var routing = Option.of(contextByKey.get("routing.tenantId")).map(tenantId -> new TenantRouting(
                tenantId,
                contextByKey.get("routing.dataSourceKey"),
                contextByKey.get("routing.schemaName"),
                contextByKey.get("routing.tableSuffix"),
                Boolean.parseBoolean(contextByKey.get("routing.rowIsolated"))
        ));
        TenantContextHolder.clear();
        TenantContextHolder.setTenantId(contextByKey.get("tenantId"));
        TenantContextHolder.setDatabase(contextByKey.get("database"));
        TenantContextHolder.setSchema(contextByKey.get("schema"));
        TenantContextHolder.setRouting(routing.getOrNull());
    }
}
