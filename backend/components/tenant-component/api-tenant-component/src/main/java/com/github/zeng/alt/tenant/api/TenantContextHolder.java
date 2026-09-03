package com.github.zeng.alt.tenant.api;


import lombok.extern.apachecommons.CommonsLog;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年12月03日 21:09
 */
@CommonsLog
public final class TenantContextHolder {


    private TenantContextHolder() {}

    private static final InheritableThreadLocal<String> currentTenant = new InheritableThreadLocal<>();
    private static final InheritableThreadLocal<String> currentDatabase = new InheritableThreadLocal<>();
    private static final InheritableThreadLocal<String> currentSchema = new InheritableThreadLocal<>();
    private static final InheritableThreadLocal<TenantRouting> currentRouting = new InheritableThreadLocal<>();

    public static void setTenantId(String tenantId) {
        if (log.isDebugEnabled()) {
            log.debug("Setting tenantId to " + tenantId);
        }
        currentTenant.set(tenantId);
    }

    public static String getTenantId() {
        return currentTenant.get();
    }

    public static void setDatabase(String database) {
        if (log.isDebugEnabled()) {
            log.debug("Setting database to " + database);
        }
        currentDatabase.set(database);
    }

    public static String getDatabase() {
        return currentDatabase.get();
    }

    public static void setSchema(String schema) {
        if (log.isDebugEnabled()) {
            log.debug("Setting schema to " + schema);
        }
        currentSchema.set(schema);
    }

    public static String getSchema() {
        return currentSchema.get();
    }

    /**
     * 缓存本次请求解析出的完整路由，避免每次取连接都重新解析。
     *
     * @param routing 租户路由，可为 null
     */
    public static void setRouting(TenantRouting routing) {
        currentRouting.set(routing);
    }

    /**
     * 取本次请求已解析的路由。
     *
     * @return 路由，未解析时返回 null
     */
    public static TenantRouting getRouting() {
        return currentRouting.get();
    }

    public static void clear(){
        currentTenant.remove();
        currentDatabase.remove();
        currentSchema.remove();
        currentRouting.remove();
    }

    public static void switchTenant(String tenant) {
        setTenantId(tenant);
    }
}
