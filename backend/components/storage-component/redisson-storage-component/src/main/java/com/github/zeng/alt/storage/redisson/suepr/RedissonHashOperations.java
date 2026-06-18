package com.github.zeng.alt.storage.redisson.suepr;

import com.github.zeng.alt.storage.CacheHashOperations;
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

    public RedissonHashOperations(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void put(String key, String hashKey, Object value) {
        redissonClient.getMap(key).put(hashKey, value);
    }

    @Override
    public void putAll(String key, Map<String, Object> map) {
        redissonClient.getMap(key).putAll(map);
    }

    @Override
    public <T> T get(String key, String hashKey) {
        RMap<String, T> map = redissonClient.getMap(key);
        return map.get(hashKey);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> entries(String key) {
        return (Map<String, Object>) (Map<?, ?>) redissonClient.getMap(key).readAllMap();
    }

    @Override
    public Set<String> keys(String key) {
        RMap<String, String> map = redissonClient.getMap(key);
        return map.readAllKeySet();
    }

    @Override
    public List<Object> values(String key) {
        return new ArrayList<>(redissonClient.getMap(key).readAllValues());
    }

    @Override
    public List<Object> multiGet(String key, String... hashKeys) {
        RMap<String, Object> map = redissonClient.getMap(key);
        return map.getAll(Set.of(hashKeys))
                .values()
                .stream()
                .toList();
    }

    @Override
    public Long delete(String key, String[] hashKeys) {
        RMap<String, Object> map = redissonClient.getMap(key);
        return map.fastRemove(hashKeys);
    }

    @Override
    public Boolean hasKey(String key, String hashKey) {
        return redissonClient.getMap(key).containsKey(hashKey);
    }

    @Override
    public Long size(String key) {
        return (long) redissonClient.getMap(key).size();
    }

    @Override
    public Long increment(String key, String hashKey, long delta) {
        RMap<String, Long> map = redissonClient.getMap(key);
        return map.addAndGet(hashKey, delta);
    }
}
