package com.github.zeng.alt.camunda.identity.remote.config;

import com.github.zeng.alt.camunda.identity.api.CamundaTenantSource;
import com.github.zeng.alt.camunda.identity.api.CamundaUserGroupSource;
import com.github.zeng.alt.camunda.identity.api.config.CamundaIdentityAutoConfiguration;
import com.github.zeng.alt.camunda.identity.remote.client.AdminIdentityClient;
import com.github.zeng.alt.camunda.identity.remote.client.RemoteCamundaTenantSource;
import com.github.zeng.alt.camunda.identity.remote.client.RemoteCamundaUserGroupSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Remote Camunda identity source auto configuration.
 */
@AutoConfiguration
@ConditionalOnClass(CamundaUserGroupSource.class)
@AutoConfigureBefore(CamundaIdentityAutoConfiguration.class)
@EnableConfigurationProperties(RemoteCamundaIdentityProperties.class)
@ConditionalOnProperty(prefix = "alt.camunda.identity.admin", name = "enabled", havingValue = "true", matchIfMissing = false)
public class RemoteCamundaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "camundaIdentityWebClient")
    public WebClient camundaIdentityWebClient(RemoteCamundaIdentityProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public AdminIdentityClient adminIdentityClient(WebClient camundaIdentityWebClient) {
        return new AdminIdentityClient(camundaIdentityWebClient);
    }

    @Bean
    @ConditionalOnMissingBean(CamundaUserGroupSource.class)
    public CamundaUserGroupSource remoteCamundaUserGroupSource(AdminIdentityClient client) {
        return new RemoteCamundaUserGroupSource(client);
    }

    @Bean
    @ConditionalOnMissingBean(CamundaTenantSource.class)
    public CamundaTenantSource remoteCamundaTenantSource(AdminIdentityClient client) {
        return new RemoteCamundaTenantSource(client);
    }
}
