package com.github.zeng.alt.workflow.mapper;

import com.github.zeng.alt.workflow.entity.FormConfigVersionEntity;
import com.github.zeng.alt.workflow.model.FormConfigVersionVO;
import com.github.zeng.alt.workflow.repository.FormConfigVersionListProjection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * 配置表单版本 MapStruct 映射
 *
 * @author zengAlt
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = OptionalMapper.class
)
public interface FormConfigVersionMapper {

    FormConfigVersionVO toVO(FormConfigVersionEntity entity);

    @Mapping(target = "fields", ignore = true)
    FormConfigVersionVO toVO(FormConfigVersionListProjection projection);
}
