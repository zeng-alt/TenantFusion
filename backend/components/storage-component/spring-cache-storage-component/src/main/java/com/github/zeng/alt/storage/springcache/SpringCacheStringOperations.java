package com.github.zeng.alt.storage.springcache;

import com.github.zeng.alt.storage.api.CacheStringOperations;
import com.github.zeng.alt.storage.api.KeyPrefixStrategy;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

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

    public SpringCacheStringOperations(CacheManager cacheManager, KeyPrefixStrategy keyPrefixStrategy) {
        this.cacheManager = cacheManager;
        this.keyPrefixStrategy = keyPrefixStrategy;
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
    public void set(String key, String value) {
        Cache cache = getCache();
        if (cache != null) {
            cache.put(wrap(key), value);
        }
    }

    @Override
    public void set(String key, String value, long timeout, TimeUnit unit) {
        // Spring Cache 不支持在 put 时指定过期时间
        set(key, value);
    }

    @Override
    public String get(String key) {
        Cache cache = getCache();
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(wrap(key));
            if (wrapper != null) {
                Object value = wrapper.get();
                return value instanceof String ? (String) value : null;
            }
        }
        return null;
    }

    @Override
    public Boolean setIfAbsent(String key, String value) {
        Cache cache = getCache();
        if (cache != null) {
            return cache.putIfAbsent(wrap(key), value) == null;
        }
        return false;
    }

    @Override
    public Boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
        // Spring Cache 不支持在 putIfAbsent 时指定过期时间
        return setIfAbsent(key, value);
    }

    @Override
    public Boolean expire(String key, long timeout, TimeUnit unit) {
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
    public Long increment(String key, long delta) {
        // Spring Cache 不支持原子递增操作
        Cache cache = getCache();
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(wrap(key));
            long newVal = delta;
            if (wrapper != null && wrapper.get() instanceof Number) {
                newVal = ((Number) wrapper.get()).longValue() + delta;
            }
            cache.put(wrap(key), String.valueOf(newVal));
            return newVal;
        }
        return delta;
    }
}
