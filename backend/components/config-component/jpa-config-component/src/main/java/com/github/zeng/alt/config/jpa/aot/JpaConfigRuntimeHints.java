package com.github.zeng.alt.config.jpa.aot;

import com.github.zeng.alt.config.server.entity.ConfigAppEntity;
import com.github.zeng.alt.config.server.entity.ConfigClientInstanceEntity;
import com.github.zeng.alt.config.server.entity.ConfigHistoryEntity;
import com.github.zeng.alt.config.server.entity.ConfigInfoEntity;
import com.github.zeng.alt.config.server.entity.ConfigReleaseEntity;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * JPA 配置实体的 Native Image 提示。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public class JpaConfigRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        registerType(
            hints,
            ConfigAppEntity.class,
            ConfigInfoEntity.class,
            ConfigHistoryEntity.class,
            ConfigReleaseEntity.class,
            ConfigClientInstanceEntity.class
        );
    }

    private static void registerType(RuntimeHints hints, Class<?>... classes) {
        new BindingReflectionHintsRegistrar().registerReflectionHints(hints.reflection(), classes);
    }
}
