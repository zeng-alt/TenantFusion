package com.github.zeng.alt.storage.api;

import java.util.concurrent.TimeUnit;

/**
 * String 结构空实现，所有操作不执行任何实际逻辑
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public class NoOpCacheStringOperations implements CacheStringOperations {

    @Override
    public void set(String key, String value) {
        // no-op
    }

    @Override
    public void set(String key, String value, long timeout, TimeUnit unit) {
        // no-op
    }

    @Override
    public String get(String key) {
        return null;
    }

    @Override
    public Boolean setIfAbsent(String key, String value) {
        return false;
    }

    @Override
    public Boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
        return false;
    }

    @Override
    public Boolean expire(String key, long timeout, TimeUnit unit) {
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
