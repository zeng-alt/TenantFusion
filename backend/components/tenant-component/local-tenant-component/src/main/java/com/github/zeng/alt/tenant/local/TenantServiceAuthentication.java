package com.github.zeng.alt.tenant.local;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * 服务间调用身份：持有合法 {@code X-Tenant-Service-Key} 的调用方。
 * <p>
 * 权限只有一个 {@code TENANT_SYNC}，且只被
 * {@code TenantServiceAuthorizationManager} 认——限定在租户元数据端点，
 * 拿着 service-key 也打不开其它接口（更不是超管）。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
public class TenantServiceAuthentication extends AbstractAuthenticationToken {

    /** 服务身份唯一权限 */
    public static final String TENANT_SYNC_AUTHORITY = "TENANT_SYNC";

    public TenantServiceAuthentication() {
        super(List.of(new SimpleGrantedAuthority(TENANT_SYNC_AUTHORITY)));
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return "tenant-sync-service";
    }
}
