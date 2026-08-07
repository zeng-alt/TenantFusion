package com.github.zeng.alt.workflow.repository;

import com.github.zeng.alt.domain.base.BaseRepository;
import com.github.zeng.alt.workflow.entity.FormConfigVersionEntity;
import com.github.zeng.alt.workflow.model.FormConfigVersionStatus;
import io.vavr.control.Option;

import java.util.List;
import java.util.Optional;

/**
 * 配置表单版本 Repository
 *
 * @author zengAlt
 */
public interface FormConfigVersionRepository extends BaseRepository<FormConfigVersionEntity, Long> {

    Option<FormConfigVersionListProjection> findByFormConfigIdAndVersion(Long formConfigId, Integer version);

    List<FormConfigVersionEntity> findByFormConfigIdOrderByVersionDesc(Long formConfigId);

    List<FormConfigVersionListProjection> findProjectionByFormConfigIdOrderByVersionDesc(Long formConfigId);

    Optional<FormConfigVersionEntity> findFirstByFormConfigIdAndCurrentTrue(Long formConfigId);

    Optional<FormConfigVersionEntity> findFirstByFormConfigIdAndStatusOrderByVersionDesc(Long formConfigId, FormConfigVersionStatus status);

    List<FormConfigVersionEntity> findByFormConfigIdAndStatus(Long formConfigId, FormConfigVersionStatus status);

    void deleteByFormConfigId(Long formConfigId);
}
