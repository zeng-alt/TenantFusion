package com.github.zeng.alt.config.server.service;

import com.github.zeng.alt.config.model.ConfigItemDTO;
import com.github.zeng.alt.config.storage.model.ConfigReleaseDto;
import com.github.zeng.alt.config.server.model.ConfigPublishCmd;
import com.github.zeng.alt.config.server.model.ConfigRollbackCmd;
import io.vavr.control.Either;
import java.util.List;

public interface ConfigPublishService {
    Either<String, ConfigReleaseDto> publish(ConfigPublishCmd command);

    Either<String, ConfigReleaseDto> rollback(ConfigRollbackCmd command);

    Either<String, List<ConfigItemDTO>> getPublishedConfigs(String appCode);

    ConfigReleaseDto publish(Long appId, List<Long> configIds, String releaseNote);

    ConfigReleaseDto rollback(Long appId);
}
