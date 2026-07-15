package com.github.zeng.alt.config.server.repository;

import com.github.zeng.alt.config.server.entity.ConfigInfoEntity;
import com.github.zeng.alt.domain.base.BaseRepository;
import io.vavr.control.Option;

import java.util.List;

public interface ConfigInfoRepository extends BaseRepository<ConfigInfoEntity, Long> {

    Option<ConfigInfoEntity> findByAppIdAndDataIdAndGroupName(Long appId, String dataId, String groupName);

    List<ConfigInfoEntity> findByAppIdAndDataIdIn(Long appId, List<String> dataIds);

    List<ConfigInfoEntity> findByAppId(Long appId);

    List<ConfigInfoEntity> findByAppIdAndStatus(Long appId, String status);
}
