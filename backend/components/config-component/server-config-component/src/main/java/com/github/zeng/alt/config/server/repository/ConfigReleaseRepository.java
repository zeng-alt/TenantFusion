package com.github.zeng.alt.config.server.repository;

import com.github.zeng.alt.config.server.entity.ConfigReleaseEntity;
import com.github.zeng.alt.domain.base.BaseRepository;
import io.vavr.control.Option;

public interface ConfigReleaseRepository extends BaseRepository<ConfigReleaseEntity, Long> {

    Option<ConfigReleaseEntity> findTopByAppIdOrderByReleaseIdDesc(Long appId);
}
