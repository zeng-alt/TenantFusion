package com.github.zeng.alt.config.server.mapper;

import com.github.zeng.alt.config.model.ConfigItemDTO;
import com.github.zeng.alt.config.server.model.ConfigAppCmd;
import com.github.zeng.alt.config.server.model.ConfigCreateCmd;
import com.github.zeng.alt.config.storage.model.ConfigAppDto;
import com.github.zeng.alt.config.storage.model.ConfigEntryDto;
import com.github.zeng.alt.config.storage.model.ConfigHistoryDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * 配置命令、存储模型与客户端模型映射。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ConfigMapper {
    ConfigItemDTO toDto(ConfigEntryDto config);

    @Mapping(target = "group", source = "group", defaultValue = "DEFAULT_GROUP")
    @Mapping(target = "format", source = "format", defaultValue = "properties")
    @Mapping(target = "status", constant = "draft")
    @Mapping(target = "version", constant = "0")
    ConfigEntryDto toEntry(ConfigCreateCmd command);

    @Mapping(target = "status", constant = "1")
    ConfigAppDto toApp(ConfigAppCmd command);

    @Mapping(target = "historyId", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    ConfigHistoryDto toHistory(ConfigEntryDto config);
}
