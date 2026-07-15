package com.github.zeng.alt.config.server.config;

import com.github.zeng.alt.config.server.aot.ConfigServerRuntimeHints;
import com.github.zeng.alt.config.server.entity.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ImportRuntimeHints;

@AutoConfiguration
@ConditionalOnProperty(prefix = "config.server", name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigurationPackage(basePackageClasses = {
        ConfigAppEntity.class,
        ConfigInfoEntity.class,
        ConfigHistoryEntity.class,
        ConfigReleaseEntity.class,
        ConfigClientInstanceEntity.class
})
@ImportRuntimeHints(ConfigServerRuntimeHints.class)
public class ServerConfigAutoConfiguration {
}
