package com.github.zeng.alt.storage.api;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class GuavaCacheStringOperations implements CacheStringOperations {

    private final Cache<String, Object> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    // 用于 pattern 删除索引
    private final Set<String> keyIndex = ConcurrentHashMap.newKeySet();

    private final AtomicLong counter = new AtomicLong(0);

    // ---------------- set ----------------

    @Override
    public <T> void set(String key, T value) {
        cache.put(key, value);
        keyIndex.add(key);
    }

    @Override
    public <T> void set(String key, T value, Duration duration) {
        cache.put(key, value);
        keyIndex.add(key);

        // Guava 不支持 per-key TTL，这里忽略或交给全局 TTL
    }

    // ---------------- get ----------------

    @Override
    public <T> T get(String key, Class<T> clazz) {
        Object value = cache.getIfPresent(key);
        return clazz.cast(value);
    }

    // ---------------- setIfAbsent ----------------

    @Override
    public <T> Boolean setIfAbsent(String key, T value) {
        if (cache.getIfPresent(key) == null) {
            cache.put(key, value);
            keyIndex.add(key);
            return true;
        }
        return false;
    }

    @Override
    public <T> Boolean setIfAbsent(String key, T value, Duration duration) {
        return setIfAbsent(key, value);
    }

    // ---------------- expire（Guava限制） ----------------

    @Override
    public Boolean expire(String key, Duration duration) {
        // Guava 不支持单 key 过期修改
        return false;
    }

    @Override
    public Long getExpire(String key) {
        return -1L;
    }

    // ---------------- delete ----------------

    @Override
    public Boolean delete(String key) {
        cache.invalidate(key);
        keyIndex.remove(key);
        return true;
    }

    @Override
    public long delete(String... keys) {
        long count = 0;

        for (String key : keys) {
            cache.invalidate(key);
            keyIndex.remove(key);
            count++;
        }

        return count;
    }

    // ---------------- pattern delete ----------------

    @Override
    public long deleteByPattern(String pattern) {

        long count = 0;

        for (String key : keyIndex.toArray(new String[0])) {

            if (match(key, pattern)) {
                cache.invalidate(key);
                keyIndex.remove(key);
                count++;
            }
        }

        return count;
    }

    // ---------------- hasKey ----------------

    @Override
    public Boolean hasKey(String key) {
        return cache.getIfPresent(key) != null;
    }

    // ---------------- increment ----------------

    @Override
    public Long increment(String key) {
        return increment(key, 1);
    }

    @Override
    public Long increment(String key, long delta) {

        Object val = cache.getIfPresent(key);

        long current = (val == null) ? 0 : (Long) val;
        long next = current + delta;

        cache.put(key, next);
        keyIndex.add(key);

        return counter.addAndGet(delta); // 或 return next
    }

    // ---------------- pattern match ----------------

    private boolean match(String key, String pattern) {
        // 简单实现：支持 *
        if (pattern.equals("*")) {
            return true;
        }

        if (pattern.startsWith("*") && pattern.endsWith("*")) {
            return key.contains(pattern.replace("*", ""));
        }

        if (pattern.startsWith("*")) {
            return key.endsWith(pattern.substring(1));
        }

        if (pattern.endsWith("*")) {
            return key.startsWith(pattern.substring(0, pattern.length() - 1));
        }

        return key.equals(pattern);
    }
}