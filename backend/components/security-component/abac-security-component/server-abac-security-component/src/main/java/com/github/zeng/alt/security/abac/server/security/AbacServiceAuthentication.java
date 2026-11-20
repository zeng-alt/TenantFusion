package com.github.zeng.alt.security.abac.server.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * 策略只读服务身份，不持有用户 Token、密钥或管理员权限。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public class AbacServiceAuthentication extends AbstractAuthenticationToken {
    private final String clientId;
    private final String tenant;

    public AbacServiceAuthentication(String clientId, String tenant) {
        super(List.of(new SimpleGrantedAuthority("ABAC_POLICY_READ")));
        this.clientId = clientId;
        this.tenant = tenant;
        setAuthenticated(true);
    }

    public String getTenant() {
        return tenant;
    }

    @Override
    public Object getPrincipal() {
        return clientId;
    }

    @Override
    public Object getCredentials() {
        return "";
    }
}
