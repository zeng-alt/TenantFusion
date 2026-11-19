package com.github.zeng.alt.config.client;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.github.zeng.alt.config.ConfigException;
import com.github.zeng.alt.config.model.ConfigItemDTO;
import io.vavr.control.Try;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 配置服务 HTTP 边界；只接受成功的完整快照。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class ConfigRemoteTemplate {
    private final ConfigClientProperties properties;
    private final ObjectMapper mapper;
    private final RestTemplate fetchClient;
    private final RestTemplate pollClient;

    public List<ConfigItemDTO> getAll() {
        final JsonNode response = Try.of(() -> fetchClient.getForObject(uri("fetch"), String.class))
                .mapTry(mapper::readTree)
                .getOrElseThrow(error -> new ConfigException("无法获取已发布配置"));
        if (response == null || response.path("status").asInt() != 200 || !response.path("data").isArray()) {
            throw new ConfigException("配置服务没有返回成功的配置快照");
        }
        final List<ConfigItemDTO> items = Try.of(() -> mapper.convertValue(
            response.get("data"),
            new TypeReference<List<ConfigItemDTO>>() {}
        )).getOrElseThrow(error -> new ConfigException("配置快照格式错误"));
        if (items.stream().anyMatch(item -> item == null || !StringUtils.hasText(item.getDataId())
                || item.getContent() == null)) {
            throw new ConfigException("配置快照包含无效条目");
        }
        return List.copyOf(items);
    }

    /** 超时也会返回，由调用方再次获取全量快照，补偿断线期间的变更。 */
    public void awaitChanges() {
        pollClient.postForObject(uri("listener"), null, String.class);
    }

    private URI uri(String endpoint) {
        return UriComponentsBuilder.fromUriString(properties.getServerAddr().replaceFirst("/+$", ""))
                .path("/api/config/" + endpoint)
                .queryParam("appCode", properties.getAppCode())
                .queryParam("listenTimeoutMs", properties.getLongPollTimeoutMs())
                .build().encode().toUri();
    }
}
