package com.github.zeng.alt.config.storage;

import com.github.zeng.alt.config.storage.model.ConfigAppDto;
import com.github.zeng.alt.config.storage.model.ConfigEntryDto;
import com.github.zeng.alt.config.storage.model.ConfigHistoryDto;
import com.github.zeng.alt.config.storage.model.ConfigReleaseDto;
import io.vavr.control.Option;
import java.util.List;
import java.util.function.Supplier;

/**
 * 服务端存储 SPI。实现负责分配 ID、维护版本及持久化；业务层负责发布与通知。
 * 读取结果返回不可变集合；存储故障抛出组件异常，缺失使用 Option。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public interface ConfigStorageTemplate {
    /** 原子执行操作，异常时回滚；自身事务提交后返回，也可加入宿主事务。 */
    <T> T execute(Supplier<T> action);

    ConfigAppDto createApp(ConfigAppDto app);

    Option<ConfigAppDto> getApp(Long appId);

    Option<ConfigAppDto> getApp(String appCode);

    List<ConfigAppDto> listApps();

    ConfigEntryDto createConfig(ConfigEntryDto config);

    Option<ConfigEntryDto> getConfig(Long configId);

    Option<ConfigEntryDto> getConfig(Long appId, String dataId, String group);

    List<ConfigEntryDto> listConfigs(Long appId);

    List<ConfigEntryDto> listConfigs(List<Long> configIds);

    ConfigEntryDto updateConfig(ConfigEntryDto config);

    /** 标记 deleted，保留内容和 ID，待发布时从有效快照移除。 */
    Option<ConfigEntryDto> deleteConfig(Long configId);

    ConfigHistoryDto createHistory(ConfigHistoryDto history);

    List<ConfigHistoryDto> listHistories(Long configId);

    ConfigReleaseDto createRelease(ConfigReleaseDto release);

    ConfigReleaseDto updateRelease(ConfigReleaseDto release);

    Option<ConfigReleaseDto> getLatestRelease(Long appId);

    /** 按 releaseId 倒序，最多返回 limit 条。 */
    List<ConfigReleaseDto> listRecentReleases(Long appId, int limit);
}
