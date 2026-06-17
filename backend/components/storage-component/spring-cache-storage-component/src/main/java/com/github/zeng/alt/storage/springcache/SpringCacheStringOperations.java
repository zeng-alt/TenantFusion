package com.github.zeng.alt.storage.springcache;

import com.github.zeng.alt.lock.api.LockTemplate;
import com.github.zeng.alt.storage.api.CacheStringOperations;
import com.github.zeng.alt.storage.api.KeyPrefixStrategy;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Spring Cache String 结构操作实现
 * 基于 Spring Cache 抽象实现基本键值操作
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public class SpringCacheStringOperations implements CacheStringOperations {

    private final CacheManager cacheManager;
    private final KeyPrefixStrategy keyPrefixStrategy;
    private final LockTemplate lockTemplate;

    public SpringCacheStringOperations(CacheManager cacheManager, KeyPrefixStrategy keyPrefixStrategy, LockTemplate lockTemplate) {
        this.cacheManager = cacheManager;
        this.keyPrefixStrategy = keyPrefixStrategy;
        this.lockTemplate = lockTemplate;
    }

    private String wrap(String key) {
        return keyPrefixStrategy.wrapKey(key);
    }

    private Cache getCache() {
        Cache cache = cacheManager.getCache("default");
        if (cache == null) {
            // 如果 default 缓存不存在，使用第一个可用的缓存区域
            for (String name : cacheManager.getCacheNames()) {
                cache = cacheManager.getCache(name);
                if (cache != null) break;
            }
        }
        return cache;
    }

    @Override
    public <T> void set(String key, T value) {
        Cache cache = getCache();
        if (cache != null) {
            cache.put(wrap(key), value);
        }
    }

    @Override
    public <T> void set(String key, T value, Duration duration) {
        // Spring Cache 不支持在 put 时指定过期时间
        set(key, value);
    }

    @Override
    public <T> T get(String key, Class<T> tClass) {
        Cache cache = getCache();
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(wrap(key));
            if (wrapper != null) {
                Object value = wrapper.get();
                return tClass.cast(value);
            }
        }
        return null;
    }

    @Override
    public <T> Boolean setIfAbsent(String key, T value) {
        Cache cache = getCache();
        if (cache != null) {
            return cache.putIfAbsent(wrap(key), value) == null;
        }
        return false;
    }

    @Override
    public <T> Boolean setIfAbsent(String key, T value, Duration duration) {
        // Spring Cache 不支持在 putIfAbsent 时指定过期时间
        return setIfAbsent(key, value);
    }

    @Override
    public Boolean expire(String key, Duration duration) {
        // Spring Cache 不支持单独设置过期时间
        return false;
    }

    @Override
    public Long getExpire(String key) {
        // Spring Cache 不支持获取过期时间
        return -1L;
    }

    @Override
    public Boolean delete(String key) {
        Cache cache = getCache();
        if (cache != null) {
            return cache.evictIfPresent(wrap(key));
        }
        return false;
    }

    @Override
    public long delete(String... keys) {

        if (keys == null || keys.length == 0) {
            return 0;
        }

        Cache cache = getCache();
        if (cache == null) {
            return 0;
        }

        long count = 0;

        for (String key : keys) {
            if (cache.evictIfPresent(wrap(key))) {
                count++;
            }
        }

        return count;
    }

    @Override
    public long deleteByPattern(String pattern) {
        throw new UnsupportedOperationException(
                "Pattern delete is not supported for this cache type: "
                        + getCache().getClass().getName()
        );
    }

    @Override
    public Boolean hasKey(String key) {
        Cache cache = getCache();
        if (cache != null) {
            return cache.get(wrap(key)) != null;
        }
        return false;
    }

    @Override
    public Long increment(String key) {
        return increment(key, 1L);
    }

    @Override
    public synchronized Long increment(String key, long delta) {
        return lockTemplate.execute(
                "lock:" + wrap(key),
                5,               // 最多等待 5 秒
                10,                      // 锁持有 10 秒
                TimeUnit.SECONDS,
                () -> {
                    Cache cache = getCache();
                    if (cache != null) {
                        Cache.ValueWrapper wrapper = cache.get(wrap(key));
                        long newVal = delta;
                        if (wrapper != null) {
                            newVal = ((Long) wrapper.get()) + delta;
                        }
                        cache.put(wrap(key), newVal);
                        return newVal;
                    }
                    return delta;
                }
        );
    }
}
