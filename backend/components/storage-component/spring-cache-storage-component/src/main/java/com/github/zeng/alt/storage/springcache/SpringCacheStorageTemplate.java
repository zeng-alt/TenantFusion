package com.github.zeng.alt.storage.springcache;

import com.github.zeng.alt.storage.api.*;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.concurrent.TimeUnit;

/**
 * Spring Cache 实现 StorageTemplate
 * Spring Cache 是键值缓存抽象，仅支持 String 结构操作
 * List / Hash / ZSet 操作使用 NoOp 空实现（不支持复杂数据结构）
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public class SpringCacheStorageTemplate extends AbstractStorageTemplate {

    private final CacheManager cacheManager;
    private final Cache cache;

    private final CacheStringOperations stringOps;
    private final CacheListOperations listOps;
    private final CacheHashOperations hashOps;
    private final CacheZSetOperations zSetOps;

    public SpringCacheStorageTemplate(CacheManager cacheManager, KeyPrefixStrategy keyPrefixStrategy) {
        super(keyPrefixStrategy);
        this.cacheManager = cacheManager;
        this.cache = cacheManager.getCache("default");
        this.stringOps = new SpringCacheStringOperations(cacheManager, keyPrefixStrategy);
        this.listOps = new NoOpCacheListOperations();
        this.hashOps = new NoOpCacheHashOperations();
        this.zSetOps = new NoOpCacheZSetOperations();
    }

    @Override
    public CacheStringOperations opsForString() {
        return stringOps;
    }

    @Override
    public CacheListOperations opsForList() {
        return listOps;
    }

    @Override
    public CacheHashOperations opsForHash() {
        return hashOps;
    }

    @Override
    public CacheZSetOperations opsForZSet() {
        return zSetOps;
    }

    @Override
    public Boolean delete(String key) {
        Cache cache = getCache();
        if (cache != null) {
            return cache.evictIfPresent(wrapKey(key));
        }
        return false;
    }

    @Override
    public Boolean hasKey(String key) {
        Cache cache = getCache();
        if (cache != null) {
            return cache.get(wrapKey(key)) != null;
        }
        return false;
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

    private Cache getCache() {
        return cache;
    }
}
