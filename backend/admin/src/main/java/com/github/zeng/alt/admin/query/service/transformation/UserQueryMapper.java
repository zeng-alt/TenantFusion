package com.github.zeng.alt.admin.query.service.transformation;

import com.github.zeng.alt.admin.infrastructure.entity.User;
import com.github.zeng.alt.admin.infrastructure.projection.UserDto;
import com.github.zeng.alt.admin.query.api.dto.UserInfoDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * 用户查询结果映射，列表沿用原有 DTO 结构。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserQueryMapper {
    UserDto toDto(User user);
    @Mapping(target = "deptName", ignore = true)
    UserInfoDto toInfoDto(User user);
}
