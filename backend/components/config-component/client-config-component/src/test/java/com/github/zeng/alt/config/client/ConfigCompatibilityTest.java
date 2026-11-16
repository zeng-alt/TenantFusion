package com.github.zeng.alt.config.client;

import com.github.zeng.alt.config.ConfigException;
import com.github.zeng.alt.config.model.ConfigItemDTO;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 旧接口的应用边界和默认分组兼容测试。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class ConfigCompatibilityTest {
    @TempDir
    Path directory;

    @Test
    void rejectsOtherApplicationsAndKeepsLegacyBatchReadsInDefaultGroup() {
        final var cache = new ConfigCacheManager(directory.toString(), "shop");
        cache.updateAll(List.of(
            new ConfigItemDTO("enabled", "DEFAULT_GROUP", "true", "text"),
            new ConfigItemDTO("enabled", "payment", "false", "text")
        ));
        final var service = new ConfigClientServiceBean(cache);

        assertThatThrownBy(() -> service.getConfig("other", "enabled", "payment")).isInstanceOf(ConfigException.class);
        assertThat(service.getConfigs("shop", List.of("enabled", "enabled")).get("enabled").getContent()).isEqualTo("true");
        assertThat(service.getAllConfigs("shop")).hasSize(1);
        assertThat(service.getConfig("shop", "enabled", "payment").getContent()).isEqualTo("false");
    }
}
