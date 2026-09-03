package com.github.zeng.alt.tenant.api;


import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年12月03日 21:19
 */
public class TenantAwareTaskDecorator implements TaskDecorator {

    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable runnable) {
        String tenantId = TenantContextHolder.getTenantId();
        String database = TenantContextHolder.getDatabase();
        String schema = TenantContextHolder.getSchema();
        TenantRouting routing = TenantContextHolder.getRouting();
        return () -> {
            try {
                TenantContextHolder.setTenantId(tenantId);
                TenantContextHolder.setDatabase(database);
                TenantContextHolder.setSchema(schema);
                TenantContextHolder.setRouting(routing);
                runnable.run();
            } finally {
                TenantContextHolder.clear();
            }
        };
    }
}
