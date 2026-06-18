package com.github.zeng.alt.storage;

import java.time.Duration;

/**
 * String 结构空实现，所有操作不执行任何实际逻辑
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public class NoOpCacheStringOperations implements CacheStringOperations {

    @Override
    public <T> void set(String key, T value) {
        // no-op
    }

    @Override
    public <T> void set(String key, T value, Duration duration) {
        // no-op
    }

    @Override
    public <T> T get(String key, Class<T> tClass) {
        return null;
    }

    @Override
    public <T> Boolean setIfAbsent(String key, T value) {
        return false;
    }

    @Override
    public <T> Boolean setIfAbsent(String key, T value, Duration duration) {
        return false;
    }

    @Override
    public Boolean expire(String key, Duration duration) {
        return false;
    }

    @Override
    public Long getExpire(String key) {
        return -2L;
    }

    @Override
    public Boolean delete(String key) {
        return false;
    }

    @Override
    public long delete(String... keys) {
        return 0;
    }

    @Override
    public long deleteByPattern(String pattern) {
        return 0;
    }

    @Override
    public Boolean hasKey(String key) {
        return false;
    }

    @Override
    public Long increment(String key) {
        return 0L;
    }

    @Override
    public Long increment(String key, long delta) {
        return 0L;
    }
}
