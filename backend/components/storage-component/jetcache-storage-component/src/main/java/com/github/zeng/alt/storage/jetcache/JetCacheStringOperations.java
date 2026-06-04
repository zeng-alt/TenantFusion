package com.github.zeng.alt.storage.jetcache;

import com.alicp.jetcache.Cache;
import com.github.zeng.alt.storage.api.CacheStringOperations;
import com.github.zeng.alt.storage.api.KeyPrefixStrategy;

import java.util.concurrent.TimeUnit;

/**
 * JetCache String 结构操作实现
 * 基于 JetCache Cache API 实现基本 KV 操作
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public class JetCacheStringOperations implements CacheStringOperations {

    private final Cache<String, String> cache;
    private final KeyPrefixStrategy keyPrefixStrategy;

    public JetCacheStringOperations(Cache<String, String> cache, KeyPrefixStrategy keyPrefixStrategy) {
        this.cache = cache;
        this.keyPrefixStrategy = keyPrefixStrategy;
    }

    private String wrap(String key) {
        return keyPrefixStrategy.wrapKey(key);
    }

    @Override
    public void set(String key, String value) {
        cache.put(wrap(key), value);
    }

    @Override
    public void set(String key, String value, long timeout, TimeUnit unit) {
        cache.put(wrap(key), value, timeout, unit);
    }

    @Override
    public String get(String key) {
        return cache.get(wrap(key));
    }

    @Override
    public Boolean setIfAbsent(String key, String value) {
        return cache.putIfAbsent(wrap(key), value);
    }

    @Override
    public Boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
        return cache.putIfAbsent(wrap(key), value, timeout, unit);
    }

    @Override
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        String value = cache.get(wrap(key));
        if (value != null) {
            cache.put(wrap(key), value, timeout, unit);
            return true;
        }
        return false;
    }

    @Override
    public Long getExpire(String key) {
        // JetCache 不提供获取 TTL 的 API
        return -1L;
    }

    @Override
    public Boolean delete(String key) {
        return cache.remove(wrap(key));
    }

    @Override
    public Boolean hasKey(String key) {
        return cache.containsKey(wrap(key));
    }

    @Override
    public Long increment(String key) {
        return increment(key, 1L);
    }

    @Override
    public Long increment(String key, long delta) {
        // JetCache 不直接支持原子递增，通过 computeIfAbsent 实现
        return cache.computeIfAbsent(wrap(key), k -> "0")
                .map(v -> {
                    long newVal = Long.parseLong(v) + delta;
                    cache.put(wrap(key), String.valueOf(newVal));
                    return newVal;
                })
                .orElse(delta);
    }
}
