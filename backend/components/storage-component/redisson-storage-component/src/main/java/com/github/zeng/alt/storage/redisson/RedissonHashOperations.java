package com.github.zeng.alt.storage.redisson;

import com.github.zeng.alt.storage.api.CacheHashOperations;
import com.github.zeng.alt.storage.api.KeyPrefixStrategy;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.util.*;
import java.util.stream.Collectors;

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
        return keyPrefixStrategy.wrapKey(key);
    }

    @Override
    public void put(String key, String hashKey, String value) {
        redissonClient.getMap(wrap(key)).put(hashKey, value);
    }

    @Override
    public void putAll(String key, Map<String, String> map) {
        redissonClient.getMap(wrap(key)).putAll(map);
    }

    @Override
    @SuppressWarnings("unchecked")
    public String get(String key, String hashKey) {
        return (String) redissonClient.getMap(wrap(key)).get(hashKey);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> entries(String key) {
        return (Map<String, String>) (Map<?, ?>) redissonClient.getMap(wrap(key)).readAllMap();
    }

    @Override
    public Set<String> keys(String key) {
        return redissonClient.getMap(wrap(key)).readAllKeySet();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> values(String key) {
        return new ArrayList<>((Collection<String>) (Collection<?>) redissonClient.getMap(wrap(key)).readAllValues());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> multiGet(String key, String... hashKeys) {
        RMap<String, Object> map = redissonClient.getMap(wrap(key));
        return map.getAll(Set.of(hashKeys)).values().stream()
                .map(v -> (String) v)
                .collect(Collectors.toList());
    }

    @Override
    public Long delete(String key, String... hashKeys) {
        return (long) redissonClient.getMap(wrap(key)).fastRemove(hashKeys);
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
        return redissonClient.getMap(wrap(key)).addAndGet(hashKey, delta);
    }
}
