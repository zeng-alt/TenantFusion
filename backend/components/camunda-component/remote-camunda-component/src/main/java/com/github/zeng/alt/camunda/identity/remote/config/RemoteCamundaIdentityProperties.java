package com.github.zeng.alt.camunda.identity.remote.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 远程 Camunda 身份源配置。
 */
@Data
@ConfigurationProperties(prefix = "alt.camunda.identity.admin")
public class RemoteCamundaIdentityProperties {

    /**
     * admin 服务基地址。
     */
    private String baseUrl = "http://127.0.0.1:8080";
}
