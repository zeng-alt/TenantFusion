package com.github.zeng.alt.config.server.repository;

import com.github.zeng.alt.config.server.entity.ConfigClientInstanceEntity;
import com.github.zeng.alt.domain.base.BaseRepository;

import java.util.Optional;

public interface ConfigClientInstanceRepository extends BaseRepository<ConfigClientInstanceEntity, Long> {

    Optional<ConfigClientInstanceEntity> findByAppCodeAndInstanceId(String appCode, String instanceId);
}
