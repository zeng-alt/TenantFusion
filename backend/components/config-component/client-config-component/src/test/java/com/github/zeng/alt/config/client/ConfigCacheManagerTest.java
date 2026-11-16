package com.github.zeng.alt.config.client;

import com.github.zeng.alt.config.model.ConfigItemDTO;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 配置缓存的分组与重启恢复测试。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class ConfigCacheManagerTest {
    @TempDir
    Path directory;

    @Test
    void keepsSameKeyInDifferentGroups() {
        final var cache = new ConfigCacheManager(directory.toString(), "shop");
        cache.updateAll(List.of(
            new ConfigItemDTO("enabled", "payment", "true", "text"),
            new ConfigItemDTO("enabled", "delivery", "false", "text")
        ));

        assertThat(cache.get("enabled", "payment").getContent()).isEqualTo("true");
        assertThat(cache.get("enabled", "delivery").getContent()).isEqualTo("false");
    }

    @Test
    void restoresIdsVersionsAndArbitraryContentAfterRestart() {
        final var item = new ConfigItemDTO("message", "DEFAULT_GROUP", "template={name\n\"你好\"", "text");
        item.setConfigId(17L);
        item.setVersion(3);
        item.setAppCode("shop");
        new ConfigCacheManager(directory.toString(), "shop").updateAll(List.of(item));

        final var restored = new ConfigCacheManager(directory.toString(), "shop").get("message");

        assertThat(restored).usingRecursiveComparison().isEqualTo(item);
    }
}
