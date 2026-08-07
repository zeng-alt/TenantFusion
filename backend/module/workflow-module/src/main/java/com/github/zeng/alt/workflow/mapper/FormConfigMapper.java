package com.github.zeng.alt.workflow.mapper;

import com.github.zeng.alt.workflow.entity.FormConfigEntity;
import com.github.zeng.alt.workflow.model.FormConfigCreateCmd;
import com.github.zeng.alt.workflow.model.FormConfigUpdateCmd;
import com.github.zeng.alt.workflow.model.FormConfigVO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * 配置表单 MapStruct 映射
 *
 * @author zengAlt
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = OptionalMapper.class
)
public interface FormConfigMapper {

    FormConfigVO toVO(FormConfigEntity entity);

    FormConfigEntity toEntity(FormConfigCreateCmd cmd);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void merge(FormConfigUpdateCmd cmd, @MappingTarget FormConfigEntity entity);
}
