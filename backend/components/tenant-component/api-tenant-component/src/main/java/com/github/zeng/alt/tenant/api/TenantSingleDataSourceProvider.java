package com.github.zeng.alt.tenant.api;


import java.util.Optional;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年12月27日 21:18
 */
public interface TenantSingleDataSourceProvider {

    public Optional<Tenant> findById(String id);
}
