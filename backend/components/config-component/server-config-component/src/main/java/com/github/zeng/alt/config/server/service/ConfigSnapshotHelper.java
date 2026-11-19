package com.github.zeng.alt.config.server.service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.github.zeng.alt.config.ConfigException;
import com.github.zeng.alt.config.model.ConfigItemDTO;
import com.github.zeng.alt.config.storage.model.ConfigEntryDto;
import com.github.zeng.alt.config.server.mapper.ConfigMapper;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * 发布快照编解码。历史无快照的发布记录需要重新发布才能供客户端读取。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class ConfigSnapshotHelper {
    private final ObjectMapper mapper;
    private final ConfigMapper entities;

    public ConfigItemDTO toDto(ConfigEntryDto entity, String appCode) {
        final ConfigItemDTO item = entities.toDto(entity);
        item.setAppCode(appCode);
        return item;
    }

    public List<ConfigItemDTO> read(String snapshot) {
        return Option.of(snapshot).filter(StringUtils::hasText)
                .map(json -> Try.of(() -> mapper.readValue(json, new TypeReference<List<ConfigItemDTO>>() {}))
                        .getOrElseThrow(error -> new ConfigException("发布快照无法读取")))
                .map(List::copyOf).getOrElse(List.of());
    }

    public String write(List<ConfigItemDTO> items) {
        return Try.of(() -> mapper.writeValueAsString(items))
                .getOrElseThrow(error -> new ConfigException("发布快照无法保存"));
    }
}
