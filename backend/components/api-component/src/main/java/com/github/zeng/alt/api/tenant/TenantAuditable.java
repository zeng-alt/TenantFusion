package com.github.zeng.alt.api.tenant;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年11月13日 10:31
 */
public interface TenantAuditable {

    default String getTenantBy() {
        return "master";
    }

    /**
     * Sets the user who created this entity.
     *
     * @param tenantBy the creating entity to set
     */
    default void setTenantBy(String tenantBy) {}
}
