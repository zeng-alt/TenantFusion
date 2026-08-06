package com.github.zeng.alt.workflow.mapper;

import com.github.zeng.alt.workflow.entity.FormTemplateEntity;
import com.github.zeng.alt.workflow.model.FormTemplateCreateCmd;
import com.github.zeng.alt.workflow.model.FormTemplateUpdateCmd;
import com.github.zeng.alt.workflow.model.FormTemplateVO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * 表单模板 MapStruct 映射：实体 ↔ VO / 命令。
 *
 * @author zengAlt
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = OptionalMapper.class
)
public interface FormTemplateMapper {

    /** 实体 → VO */
    FormTemplateVO toVO(FormTemplateEntity entity);

    /** 创建命令 → 实体 */
    FormTemplateEntity toEntity(FormTemplateCreateCmd cmd);

    /** 更新命令合并到实体（部分更新：null 字段不覆盖） */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void merge(FormTemplateUpdateCmd cmd, @MappingTarget FormTemplateEntity entity);
}