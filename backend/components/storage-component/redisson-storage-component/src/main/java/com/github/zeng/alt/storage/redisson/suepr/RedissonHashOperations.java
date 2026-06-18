package com.github.zeng.alt.storage.redisson.suepr;

import com.github.zeng.alt.storage.CacheHashOperations;
import com.github.zeng.alt.storage.KeyPrefixStrategy;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.util.*;

/**
 * Redisson Hash 结构操作实现
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public class RedissonHashOperations implements CacheHashOperations {

    private final RedissonClient redissonClient;
    private final KeyPrefixStrategy keyPrefixStrategy;

    public RedissonHashOperations(RedissonClient redissonClient, KeyPrefixStrategy keyPrefixStrategy) {
        this.redissonClient = redissonClient;
        this.keyPrefixStrategy = keyPrefixStrategy;
    }

    private String wrap(String key) {
        return keyPrefixStrategy.map(key);
    }

    @Override
    public void put(String key, String hashKey, Object value) {
        redissonClient.getMap(wrap(key)).put(hashKey, value);
    }

    @Override
    public void putAll(String key, Map<String, Object> map) {
        redissonClient.getMap(wrap(key)).putAll(map);
    }

    @Override
    public <T> T get(String key, String hashKey) {
        RMap<String, T> map = redissonClient.getMap(wrap(key));
        return map.get(hashKey);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> entries(String key) {
        return (Map<String, Object>) (Map<?, ?>) redissonClient.getMap(wrap(key)).readAllMap();
    }

    @Override
    public Set<String> keys(String key) {
        RMap<String, String> map = redissonClient.getMap(wrap(key));
        return map.readAllKeySet();
    }

    @Override
    public List<Object> values(String key) {
        return new ArrayList<>(redissonClient.getMap(wrap(key)).readAllValues());
    }

    @Override
    public List<Object> multiGet(String key, String... hashKeys) {
        RMap<String, Object> map = redissonClient.getMap(wrap(key));
        return map.getAll(Set.of(hashKeys))
                .values()
                .stream()
                .toList();
    }

    @Override
    public Long delete(String key, String[] hashKeys) {
        RMap<String, Object> map = redissonClient.getMap(wrap(key));
        return map.fastRemove(hashKeys);
    }

    @Override
    public Boolean hasKey(String key, String hashKey) {
        return redissonClient.getMap(wrap(key)).containsKey(hashKey);
    }

    @Override
    public Long size(String key) {
        return (long) redissonClient.getMap(wrap(key)).size();
    }

    @Override
    public Long increment(String key, String hashKey, long delta) {
        RMap<String, Long> map = redissonClient.getMap(wrap(key));
        return map.addAndGet(hashKey, delta);
    }
}
