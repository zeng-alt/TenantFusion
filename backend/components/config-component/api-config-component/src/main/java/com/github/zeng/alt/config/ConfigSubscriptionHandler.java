package com.github.zeng.alt.config;

/**
 * 可重复关闭的业务配置订阅句柄。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface ConfigSubscriptionHandler extends AutoCloseable {
    @Override
    void close();
}
