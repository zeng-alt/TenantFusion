package com.github.zeng.alt.workflow.mapper;

import com.github.zeng.alt.workflow.entity.WorkflowEntity;
import com.github.zeng.alt.workflow.model.WorkflowCreateCmd;
import com.github.zeng.alt.workflow.model.WorkflowUpdateCmd;
import com.github.zeng.alt.workflow.model.WorkflowVO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * 流程主数据 MapStruct 映射：实体 ↔ VO / 命令。
 *
 * @author zengAlt
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = OptionalMapper.class
)
public interface WorkflowMapper {

    /** 实体 → VO */
    WorkflowVO toVO(WorkflowEntity entity);

    /** 创建命令 → 实体 */
    WorkflowEntity toEntity(WorkflowCreateCmd cmd);

    /** 更新命令合并到实体（部分更新：null 字段不覆盖） */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void merge(WorkflowUpdateCmd cmd, @MappingTarget WorkflowEntity entity);
}