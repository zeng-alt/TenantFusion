package com.github.zeng.alt.excel.context;

import com.github.zeng.alt.tenant.api.TenantContextHolder;
import com.github.zeng.alt.tenant.api.TenantRouting;

/**
 * 捕获完整租户路由，空快照也会清除执行线程上的旧租户。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class TenantExcelContextHandler implements ExcelContextHandler {

    @Override
    public Snapshot capture() {
        final Runnable captured = captureAssignment();
        return () -> {
            final Runnable previous = captureAssignment();
            captured.run();
            return previous::run;
        };
    }

    private Runnable captureAssignment() {
        final String tenantId = TenantContextHolder.getTenantId();
        final String database = TenantContextHolder.getDatabase();
        final String schema = TenantContextHolder.getSchema();
        final TenantRouting routing = TenantContextHolder.getRouting();
        return () -> {
            TenantContextHolder.clear();
            TenantContextHolder.setTenantId(tenantId);
            TenantContextHolder.setDatabase(database);
            TenantContextHolder.setSchema(schema);
            TenantContextHolder.setRouting(routing);
        };
    }
}
