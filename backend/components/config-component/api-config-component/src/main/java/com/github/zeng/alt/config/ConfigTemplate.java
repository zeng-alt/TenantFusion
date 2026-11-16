package com.github.zeng.alt.config;

import com.github.zeng.alt.config.event.ConfigChangeListener;

/**
 * 当前应用的业务配置入口；分组操作返回独立视图，不改变已有视图。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public interface ConfigTemplate {
    String DEFAULT_GROUP = "DEFAULT_GROUP";

    ConfigTemplate group(String group);

    ConfigValueTemplate opsForValue();

    ConfigObjectTemplate opsForObject();

    /** 订阅当前分组的指定配置；关闭返回的句柄可取消订阅。 */
    ConfigSubscriptionHandler subscribe(String key, ConfigChangeListener listener);
}
