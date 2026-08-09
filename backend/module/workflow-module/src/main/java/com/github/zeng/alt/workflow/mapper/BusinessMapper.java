package com.github.zeng.alt.workflow.mapper;

import com.github.zeng.alt.workflow.entity.BusinessEntity;
import com.github.zeng.alt.workflow.model.BusinessCreateCmd;
import com.github.zeng.alt.workflow.model.BusinessUpdateCmd;
import com.github.zeng.alt.workflow.model.BusinessVO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * 业务 MapStruct 映射
 *
 * @author zengAlt
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = OptionalMapper.class
)
public interface BusinessMapper {

    BusinessVO toVO(BusinessEntity entity);

    BusinessEntity toEntity(BusinessCreateCmd cmd);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void merge(BusinessUpdateCmd cmd, @MappingTarget BusinessEntity entity);
}
