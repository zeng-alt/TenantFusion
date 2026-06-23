package com.github.zeng.alt.tenant.api;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年10月24日 20:28
 */
public interface TenantDetail {

    String getTenantName();

    String getDatabase();

    String getSchema();
}
