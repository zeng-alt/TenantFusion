package com.github.zeng.alt.config.jpa.mapper;

import com.github.zeng.alt.config.server.entity.ConfigAppEntity;
import com.github.zeng.alt.config.server.entity.ConfigHistoryEntity;
import com.github.zeng.alt.config.server.entity.ConfigInfoEntity;
import com.github.zeng.alt.config.server.entity.ConfigReleaseEntity;
import com.github.zeng.alt.config.storage.model.ConfigAppDto;
import com.github.zeng.alt.config.storage.model.ConfigEntryDto;
import com.github.zeng.alt.config.storage.model.ConfigHistoryDto;
import com.github.zeng.alt.config.storage.model.ConfigReleaseDto;
import java.util.Optional;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * JPA 实体与中立存储模型的映射。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class JpaConfigMapper {
    public abstract ConfigAppDto toDto(ConfigAppEntity entity);

    public abstract ConfigAppEntity toEntity(ConfigAppDto model);

    @Mapping(target = "group", source = "groupName")
    public abstract ConfigEntryDto toDto(ConfigInfoEntity entity);

    @Mapping(target = "groupName", source = "group")
    public abstract ConfigInfoEntity toEntity(ConfigEntryDto model);

    @Mapping(target = "groupName", source = "group")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void merge(ConfigEntryDto model, @MappingTarget ConfigInfoEntity entity);

    public abstract ConfigHistoryDto toDto(ConfigHistoryEntity entity);

    public abstract ConfigHistoryEntity toEntity(ConfigHistoryDto model);

    public abstract ConfigReleaseDto toDto(ConfigReleaseEntity entity);

    public abstract ConfigReleaseEntity toEntity(ConfigReleaseDto model);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void merge(ConfigReleaseDto model, @MappingTarget ConfigReleaseEntity entity);

    /** Spring Auditable 的 Optional 在此映射为 DTO 可空字段。 */
    protected <T> T unwrap(Optional<T> value) {
        return value.orElse(null);
    }
}
