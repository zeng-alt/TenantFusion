package com.github.zeng.alt.security.abac.client.remote;

import com.github.zeng.alt.security.abac.AbacTemplate;
import com.github.zeng.alt.security.abac.client.config.AbacClientAutoConfiguration;
import com.github.zeng.alt.security.abac.client.config.AbacClientProperties;
import com.github.zeng.alt.security.abac.policy.AbacPolicyQueryService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.context.annotation.Bean;

/**
 * 远程模式注册固定的 Feign 接口；本地模式不建立远程连接或要求服务凭据。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@AutoConfiguration(before = AbacClientAutoConfiguration.class, afterName = {
        "org.springframework.cloud.openfeign.FeignAutoConfiguration",
        "com.github.zeng.alt.security.abac.server.config.AbacServerAutoConfiguration"
})
@EnableConfigurationProperties(AbacClientProperties.class)
@EnableFeignClients(clients = AbacPolicyFeignClient.class)
@LoadBalancerClient(name = "${security.abac.client.service-id:admin-application}")
@ConditionalOnMissingBean({AbacTemplate.class, AbacPolicyQueryService.class})
@ConditionalOnProperty(prefix = "security.abac.client", name = "source", havingValue = "remote")
@ConditionalOnProperty(prefix = "security.abac.client", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AbacFeignAutoConfiguration {
    @Bean
    public AbacPolicyQueryService feignAbacPolicyQueryService(AbacPolicyFeignClient client) {
        return new FeignAbacPolicyQueryService(client);
    }
}
