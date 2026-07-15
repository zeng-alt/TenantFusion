package com.github.zeng.alt.config.server.service;

import com.github.zeng.alt.config.model.ConfigItemDTO;
import com.github.zeng.alt.config.server.entity.ConfigReleaseEntity;

import java.util.List;

public interface ConfigPublishService {

    ConfigReleaseEntity publish(Long appId, List<Long> configIds, String releaseNote);

    ConfigReleaseEntity rollback(Long appId);
}
