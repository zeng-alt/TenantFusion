package com.github.zeng.alt.workflow.mapper;

import com.github.zeng.alt.workflow.entity.FormTemplateVersionEntity;
import com.github.zeng.alt.workflow.model.FormTemplateVersionVO;
import com.github.zeng.alt.workflow.repository.FormTemplateVersionListProjection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * 表单模板版本 MapStruct 映射：实体 → VO。
 * <p>
 * {@link FormTemplateVersionVO#definition}（JsonNode）由
 * {@link DefinitionJsonMapper} 从实体 definition（JSON 字符串）转换。
 *
 * @author zengAlt
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {OptionalMapper.class, DefinitionJsonMapper.class}
)
public interface FormTemplateVersionMapper {

    /** 实体 → VO（含表单定义） */
    FormTemplateVersionVO toVO(FormTemplateVersionEntity entity);

    /** 列表投影 → VO（不含表单定义，用于版本列表） */
    @Mapping(target = "definition", ignore = true)
    FormTemplateVersionVO toVO(FormTemplateVersionListProjection projection);
}