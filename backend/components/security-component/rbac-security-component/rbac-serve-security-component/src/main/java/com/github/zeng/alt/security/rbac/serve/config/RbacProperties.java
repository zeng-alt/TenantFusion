package com.github.zeng.alt.security.rbac.serve.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RBAC 服务端配置。unmatchedUrlPolicy 控制 URL 无匹配权限时放行或拒绝，默认 DENY（与历史行为一致）。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@ConfigurationProperties(prefix = "security.rbac")
public class RbacProperties {

    private UnmatchedUrlPolicy unmatchedUrlPolicy = UnmatchedUrlPolicy.DENY;

    public UnmatchedUrlPolicy getUnmatchedUrlPolicy() {
        return unmatchedUrlPolicy;
    }

    public void setUnmatchedUrlPolicy(UnmatchedUrlPolicy unmatchedUrlPolicy) {
        this.unmatchedUrlPolicy = unmatchedUrlPolicy;
    }
}