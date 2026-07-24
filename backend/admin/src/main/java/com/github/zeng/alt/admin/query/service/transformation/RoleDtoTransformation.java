package com.github.zeng.alt.admin.query.service.transformation;

import com.github.zeng.alt.admin.infrastructure.entity.Role;
import com.github.zeng.alt.admin.query.api.dto.CreateRoleDto;
import com.github.zeng.alt.admin.query.api.dto.PatchRoleDto;
import org.mapstruct.*;

/**
 * @author zengJiaJun
 * @since 2026年07月24日
 * @version 1.0
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface RoleDtoTransformation {

    Role toEntity(CreateRoleDto dto);

    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
    )
    void mergeEntity(PatchRoleDto dto, @MappingTarget Role role);
}
