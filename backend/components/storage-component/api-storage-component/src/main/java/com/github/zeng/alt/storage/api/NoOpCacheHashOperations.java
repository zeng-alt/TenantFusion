package com.github.zeng.alt.storage.api;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Hash 结构空实现，所有操作不执行任何实际逻辑
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public class NoOpCacheHashOperations implements CacheHashOperations {

    @Override
    public void put(String key, String hashKey, String value) {
        // no-op
    }

    @Override
    public void putAll(String key, Map<String, String> map) {
        // no-op
    }

    @Override
    public String get(String key, String hashKey) {
        return null;
    }

    @Override
    public Map<String, String> entries(String key) {
        return Collections.emptyMap();
    }

    @Override
    public Set<String> keys(String key) {
        return Collections.emptySet();
    }

    @Override
    public List<String> values(String key) {
        return Collections.emptyList();
    }

    @Override
    public List<String> multiGet(String key, String... hashKeys) {
        return Collections.emptyList();
    }

    @Override
    public Long delete(String key, String... hashKeys) {
        return 0L;
    }

    @Override
    public Boolean hasKey(String key, String hashKey) {
        return false;
    }

    @Override
    public Long size(String key) {
        return 0L;
    }

    @Override
    public Long increment(String key, String hashKey, long delta) {
        return 0L;
    }
}
