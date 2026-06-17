package com.github.zeng.alt.storage.jetcache;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.CacheManager;
import com.github.zeng.alt.lock.api.LockTemplate;
import com.github.zeng.alt.storage.api.*;

import java.util.concurrent.TimeUnit;

/**
 * JetCache 实现 StorageTemplate
 * JetCache 主要提供 KV 缓存能力，因此：
 * - String 操作基于 JetCache Cache API 实现
 * - List / Hash / ZSet 操作使用 NoOp 空实现（不支持）
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public class JetCacheStorageTemplate extends AbstractStorageTemplate {

    private final CacheManager cacheManager;
    private final Cache<String, Object> cache;

    private final CacheStringOperations stringOps;
    private final CacheListOperations listOps;
    private final CacheHashOperations hashOps;
    private final CacheZSetOperations zSetOps;

    public JetCacheStorageTemplate(CacheManager cacheManager, KeyPrefixStrategy keyPrefixStrategy, LockTemplate lockTemplate) {
        super(keyPrefixStrategy);
        this.cacheManager = cacheManager;
        // 使用默认区域缓存，可通过配置文件自定义
        this.cache = cacheManager.getCache("jetCacheStorage");
        this.stringOps = new JetCacheStringOperations(cache, keyPrefixStrategy, lockTemplate);
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
        return cache.remove(wrapKey(key));
    }

    @Override
    public Boolean hasKey(String key) {
        return cache.GET(wrapKey(key)).isSuccess();
    }

    @Override
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        // JetCache 的 Cache API 不支持单独设置过期时间，需要通过 put 时指定
        // 尝试读取再写入来更新 TTL
        Object value = cache.get(wrapKey(key));
        if (value != null) {
            cache.put(wrapKey(key), value, timeout, unit);
            return true;
        }
        return false;
    }

    @Override
    public Long getExpire(String key) {
        // JetCache 不提供获取剩余 TTL 的 API
        return -1L;
    }
}
