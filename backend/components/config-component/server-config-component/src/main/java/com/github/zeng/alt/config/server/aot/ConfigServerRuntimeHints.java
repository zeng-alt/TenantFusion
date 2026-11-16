package com.github.zeng.alt.config.server.aot;

import com.github.zeng.alt.config.model.ConfigItemDTO;
import com.github.zeng.alt.config.storage.model.ConfigAppDto;
import com.github.zeng.alt.config.storage.model.ConfigEntryDto;
import com.github.zeng.alt.config.storage.model.ConfigHistoryDto;
import com.github.zeng.alt.config.storage.model.ConfigReleaseDto;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * 中立配置模型的 Native Image 提示。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public class ConfigServerRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader loader) {
        hints.resources().registerPattern("db/config/*.sql");
        new BindingReflectionHintsRegistrar().registerReflectionHints(
            hints.reflection(),
            ConfigItemDTO.class,
            ConfigAppDto.class,
            ConfigEntryDto.class,
            ConfigHistoryDto.class,
            ConfigReleaseDto.class
        );
    }
}
