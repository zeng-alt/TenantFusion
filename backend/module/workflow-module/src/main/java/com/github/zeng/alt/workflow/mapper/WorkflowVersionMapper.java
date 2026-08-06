package com.github.zeng.alt.workflow.mapper;

import com.github.zeng.alt.workflow.entity.WorkflowVersionEntity;
import com.github.zeng.alt.workflow.model.FormTemplateVersionVO;
import com.github.zeng.alt.workflow.model.WorkflowVersionVO;
import com.github.zeng.alt.workflow.repository.FormTemplateVersionListProjection;
import com.github.zeng.alt.workflow.repository.WorkflowVersionListProjection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * 流程版本 MapStruct 映射：实体 → VO。
 *
 * @author zengAlt
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = OptionalMapper.class
)
public interface WorkflowVersionMapper {

    /** 实体 → VO（含 BPMN XML） */
    WorkflowVersionVO toVO(WorkflowVersionEntity entity);

    /** 列表投影 → VO（不含表单定义，用于版本列表） */
    @Mapping(target = "bpmnXml", ignore = true)
    WorkflowVersionVO toVO(WorkflowVersionListProjection projection);
}