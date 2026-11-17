package com.github.zeng.alt.log.jpa.mapper;

import com.github.zeng.alt.log.LoginInfoEvent;
import com.github.zeng.alt.log.OperLogEvent;
import com.github.zeng.alt.log.jpa.entity.LogEntity;
import com.github.zeng.alt.log.jpa.entity.LoginLogEntity;
import java.time.LocalDateTime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * 日志事件与 JPA 实体的映射；持久化时生成新的日志主键。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface JpaLogMapper {
    @Mapping(target = "id", ignore = true)
    LogEntity toEntity(OperLogEvent event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "loginTime", source = "time")
    LoginLogEntity toEntity(LoginInfoEvent event, LocalDateTime time);
}
