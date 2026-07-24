package com.github.zeng.alt.admin.query.service.transformation;

import com.github.zeng.alt.admin.infrastructure.entity.User;
import com.github.zeng.alt.admin.query.api.dto.CreateUserDto;
import com.github.zeng.alt.admin.query.api.dto.PatchUserDto;
import com.github.zeng.alt.admin.query.api.dto.ResetUserPasswordDto;
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
public interface UserDtoTransformation {

    User toEntity(CreateUserDto dto);

    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
    )
    void mergeEntity(PatchUserDto dto, @MappingTarget User user);

    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
    )
    void mergeEntity(ResetUserPasswordDto dto, @MappingTarget User user);
}
