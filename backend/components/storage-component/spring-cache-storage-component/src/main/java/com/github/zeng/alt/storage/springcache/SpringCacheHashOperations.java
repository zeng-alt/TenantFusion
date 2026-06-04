package com.github.zeng.alt.storage.springcache;

import com.github.zeng.alt.storage.api.CacheHashOperations;
import com.github.zeng.alt.storage.api.KeyPrefixStrategy;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Spring Data Redis Hash 结构操作实现
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public class SpringCacheHashOperations implements CacheHashOperations {

    private final StringRedisTemplate redisTemplate;
    private final KeyPrefixStrategy keyPrefixStrategy;

    public SpringCacheHashOperations(StringRedisTemplate redisTemplate, KeyPrefixStrategy keyPrefixStrategy) {
        this.redisTemplate = redisTemplate;
        this.keyPrefixStrategy = keyPrefixStrategy;
    }

    private String wrap(String key) {
        return keyPrefixStrategy.wrapKey(key);
    }

    private HashOperations<String, String, String> ops() {
        return redisTemplate.opsForHash();
    }

    @Override
    public void put(String key, String hashKey, String value) {
        ops().put(wrap(key), hashKey, value);
    }

    @Override
    public void putAll(String key, Map<String, String> map) {
        ops().putAll(wrap(key), map);
    }

    @Override
    public String get(String key, String hashKey) {
        return ops().get(wrap(key), hashKey);
    }

    @Override
    public Map<String, String> entries(String key) {
        return ops().entries(wrap(key));
    }

    @Override
    public Set<String> keys(String key) {
        return ops().keys(wrap(key));
    }

    @Override
    public List<String> values(String key) {
        return ops().values(wrap(key));
    }

    @Override
    public List<String> multiGet(String key, String... hashKeys) {
        return ops().multiGet(wrap(key), List.of(hashKeys));
    }

    @Override
    public Long delete(String key, String... hashKeys) {
        return ops().delete(wrap(key), (Object[]) hashKeys);
    }

    @Override
    public Boolean hasKey(String key, String hashKey) {
        return ops().hasKey(wrap(key), hashKey);
    }

    @Override
    public Long size(String key) {
        return ops().size(wrap(key));
    }

    @Override
    public Long increment(String key, String hashKey, long delta) {
        return ops().increment(wrap(key), hashKey, delta);
    }
}
