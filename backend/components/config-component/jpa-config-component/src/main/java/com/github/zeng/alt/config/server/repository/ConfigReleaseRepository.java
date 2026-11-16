package com.github.zeng.alt.config.server.repository;

import com.github.zeng.alt.config.server.entity.ConfigReleaseEntity;
import com.github.zeng.alt.domain.base.BaseRepository;
import io.vavr.control.Option;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface ConfigReleaseRepository extends BaseRepository<ConfigReleaseEntity, Long> {

    List<ConfigReleaseEntity> findByAppIdOrderByReleaseIdDesc(Long appId, Pageable pageable);

    Option<ConfigReleaseEntity> findTopByAppIdOrderByReleaseIdDesc(Long appId);
}
