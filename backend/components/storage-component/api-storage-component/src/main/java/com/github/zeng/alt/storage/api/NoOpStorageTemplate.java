package com.github.zeng.alt.storage.api;

import java.util.concurrent.TimeUnit;

/**
 * 存储模板空实现，所有操作不执行任何实际逻辑
 * 当未配置任何缓存实现时作为默认 fallback
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public class NoOpStorageTemplate implements StorageTemplate {

    private static final NoOpCacheStringOperations STRING_OPS = new NoOpCacheStringOperations();
    private static final NoOpCacheListOperations LIST_OPS = new NoOpCacheListOperations();
    private static final NoOpCacheHashOperations HASH_OPS = new NoOpCacheHashOperations();
    private static final NoOpCacheZSetOperations ZSET_OPS = new NoOpCacheZSetOperations();

    @Override
    public CacheStringOperations opsForString() {
        return STRING_OPS;
    }

    @Override
    public CacheListOperations opsForList() {
        return LIST_OPS;
    }

    @Override
    public CacheHashOperations opsForHash() {
        return HASH_OPS;
    }

    @Override
    public CacheZSetOperations opsForZSet() {
        return ZSET_OPS;
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
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return false;
    }

    @Override
    public Long getExpire(String key) {
        return -2L;
    }
}
