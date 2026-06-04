package com.github.zeng.alt.storage.api;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * List 结构空实现，所有操作不执行任何实际逻辑
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public class NoOpCacheListOperations implements CacheListOperations {

    @Override
    public Long leftPush(String key, String value) {
        return 0L;
    }

    @Override
    public Long leftPushAll(String key, String... values) {
        return 0L;
    }

    @Override
    public Long rightPush(String key, String value) {
        return 0L;
    }

    @Override
    public Long rightPushAll(String key, String... values) {
        return 0L;
    }

    @Override
    public String leftPop(String key) {
        return null;
    }

    @Override
    public String leftPop(String key, long timeout, TimeUnit unit) {
        return null;
    }

    @Override
    public String rightPop(String key) {
        return null;
    }

    @Override
    public String rightPop(String key, long timeout, TimeUnit unit) {
        return null;
    }

    @Override
    public List<String> range(String key, long start, long end) {
        return Collections.emptyList();
    }

    @Override
    public Long size(String key) {
        return 0L;
    }

    @Override
    public String index(String key, long index) {
        return null;
    }

    @Override
    public void set(String key, long index, String value) {
        // no-op
    }

    @Override
    public Long remove(String key, long count, String value) {
        return 0L;
    }

    @Override
    public void trim(String key, long start, long end) {
        // no-op
    }
}
