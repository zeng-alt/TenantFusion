package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.infrastructure.entity.User;
import io.vavr.control.Option;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * 实体到授权属性快照的唯一映射边界。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface AbacUserMapper {
    @Mapping(
            target = "id",
            source = "userId"
    )
    @Mapping(
            target = "tenant",
            source = "tenantBy"
    )
    @Mapping(
            target = "enabled",
            expression = "java(Boolean.TRUE.equals(user.getEnabled()))"
    )
    AbacUserDto toDto(User user);

    default Option<Long> toDepartment(Long deptId) {
        return Option.of(deptId);
    }
}
