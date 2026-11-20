package com.github.zeng.alt.security.core.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年12月25日 21:54
 */
@Data
@Component
@ConfigurationProperties(prefix = "security.context")
public class SecurityProperties {
    private Boolean enabledAccess = true;
    private String abacPrefix = "/";

    private AdminRole admin = new AdminRole();

    @Data
    public static class AdminRole {
        private String id = "1";
        /** 可切换租户的内置用户所属租户；未配置时禁用切换。 */
        private String tenant;
        private String code = "superAdmin";
        private String name = "超级管理员";
        private Boolean enabled = true;
    }
}
