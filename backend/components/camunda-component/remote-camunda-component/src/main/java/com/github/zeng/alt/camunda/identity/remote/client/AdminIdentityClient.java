package com.github.zeng.alt.camunda.identity.remote.client;

import com.github.zeng.alt.camunda.identity.remote.dto.PasswordValidationRequest;
import com.github.zeng.alt.camunda.identity.remote.dto.RemoteCamundaIdentityGroup;
import com.github.zeng.alt.camunda.identity.remote.dto.RemoteCamundaIdentityTenant;
import com.github.zeng.alt.camunda.identity.remote.dto.RemoteCamundaIdentityUser;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

/**
 * 调用 admin 服务的 Camunda 身份接口客户端（基于 Spring 6 WebClient）。
 */
public class AdminIdentityClient {

    private final WebClient webClient;

    public AdminIdentityClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public RemoteCamundaIdentityUser findUserByUsername(String username) {
        try {
            return webClient.get()
                    .uri("/v1/camunda-identity/users/{username}", username)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(RemoteCamundaIdentityUser.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            return null;
        }
    }

    public boolean validatePassword(String username, String rawPassword) {
        try {
            Boolean result = webClient.post()
                    .uri("/v1/camunda-identity/users/{username}/password", username)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(new PasswordValidationRequest(rawPassword))
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();
            return Boolean.TRUE.equals(result);
        } catch (WebClientResponseException.NotFound e) {
            return false;
        }
    }

    public List<RemoteCamundaIdentityGroup> findAllGroups() {
        try {
            return webClient.get()
                    .uri("/v1/camunda-identity/groups")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<RemoteCamundaIdentityGroup>>() {})
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            return List.of();
        }
    }

    public RemoteCamundaIdentityTenant findTenantById(String tenantId) {
        try {
            return webClient.get()
                    .uri("/v1/camunda-identity/tenants/{tenantId}", tenantId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(RemoteCamundaIdentityTenant.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            return null;
        }
    }

    public List<RemoteCamundaIdentityTenant> findTenantsByUsername(String username) {
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/v1/camunda-identity/tenants")
                            .queryParam("username", username)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<RemoteCamundaIdentityTenant>>() {})
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            return List.of();
        }
    }

    public List<RemoteCamundaIdentityTenant> findAllTenants() {
        try {
            return webClient.get()
                    .uri("/v1/camunda-identity/tenants")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<RemoteCamundaIdentityTenant>>() {})
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            return List.of();
        }
    }
}
