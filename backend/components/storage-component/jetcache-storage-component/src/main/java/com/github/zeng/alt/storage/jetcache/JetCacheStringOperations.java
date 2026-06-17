package com.github.zeng.alt.storage.jetcache;

import com.alicp.jetcache.Cache;
import com.github.zeng.alt.lock.api.LockTemplate;
import com.github.zeng.alt.storage.api.CacheStringOperations;
import com.github.zeng.alt.storage.api.KeyPrefixStrategy;

import java.time.Duration;
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

    private final Cache<String, Object> cache;
    private final KeyPrefixStrategy keyPrefixStrategy;
    private final LockTemplate lockTemplate;

    public JetCacheStringOperations(Cache<String, Object> cache, KeyPrefixStrategy keyPrefixStrategy, LockTemplate lockTemplate) {
        this.cache = cache;
        this.keyPrefixStrategy = keyPrefixStrategy;
        this.lockTemplate = lockTemplate;
    }

    private String wrap(String key) {
        return keyPrefixStrategy.wrapKey(key);
    }

    @Override
    public <T> void set(String key, T value) {
        cache.put(wrap(key), value);
    }

    @Override
    public <T> void set(String key, T value, Duration duration) {
        cache.put(wrap(key), value, duration.toMillis(), TimeUnit.MINUTES);
    }

    @Override
    public <T> T get(String key, Class<T> tClass) {
        return tClass.cast(cache.get(wrap(key)));
    }

    @Override
    public <T> Boolean setIfAbsent(String key, T value) {
        return cache.putIfAbsent(wrap(key), value);
    }

    @Override
    public <T> Boolean setIfAbsent(String key, T value, Duration duration) {
        return cache.PUT_IF_ABSENT(wrap(key), value, duration.toMillis(), TimeUnit.MINUTES).isSuccess();
    }

    @Override
    public Boolean expire(String key, Duration duration) {
        Object value = cache.get(wrap(key));
        if (value != null) {
            cache.put(wrap(key), value, duration.toMillis(), TimeUnit.MINUTES);
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
    public long delete(String... keys) {

        if (keys == null || keys.length == 0) {
            return 0;
        }

        long count = 0;
        for (String key : keys) {
            if (cache.remove(wrap(key))) {
                count++;
            }
        }

        return count;
    }

    @Override
    public long deleteByPattern(String pattern) {
        throw new UnsupportedOperationException(
                "Pattern delete is not supported for this cache type: "
                        + cache.getClass().getName()
        );
    }

    @Override
    public Boolean hasKey(String key) {
        return cache.GET(key).isSuccess();
    }

    @Override
    public Long increment(String key) {
        return increment(key, 1L);
    }

    @Override
    public synchronized Long increment(String key, long delta) {
        // JetCache 不直接支持原子递增
        String realKey = wrap(key);

        return lockTemplate.execute(
                "lock:" + realKey,
                5,               // 最多等待 5 秒
                10,                      // 锁持有 10 秒
                TimeUnit.SECONDS,
                () -> {
                    Object old = cache.get(realKey);
                    if (old == null) {
                        old = "0";
                    }

                    long newVal = ((Long) old) + delta;
                    cache.put(realKey, newVal);

                    return newVal;
                }
        );
    }
}
