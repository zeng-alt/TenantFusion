package com.github.zeng.alt.tenant.api;


import java.util.Collection;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年12月27日 21:07
 */
public interface TenantDataSourceProvider {

    Collection<Tenant> findAll();
}
