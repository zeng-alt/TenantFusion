package com.github.zeng.alt.config.server.repository;

import com.github.zeng.alt.config.server.entity.ConfigAppEntity;
import com.github.zeng.alt.domain.base.BaseRepository;
import io.vavr.control.Option;

import java.util.Optional;

public interface ConfigAppRepository extends BaseRepository<ConfigAppEntity, Long> {

    Option<ConfigAppEntity> findByAppCode(String appCode);
}
