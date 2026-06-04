package com.github.zeng.alt.storage.springcache;

import com.github.zeng.alt.storage.api.CacheZSetOperations;
import com.github.zeng.alt.storage.api.KeyPrefixStrategy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Set;

/**
 * Spring Data Redis ZSet（有序集合）结构操作实现
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public class SpringCacheZSetOperations implements CacheZSetOperations {

    private final StringRedisTemplate redisTemplate;
    private final KeyPrefixStrategy keyPrefixStrategy;

    public SpringCacheZSetOperations(StringRedisTemplate redisTemplate, KeyPrefixStrategy keyPrefixStrategy) {
        this.redisTemplate = redisTemplate;
        this.keyPrefixStrategy = keyPrefixStrategy;
    }

    private String wrap(String key) {
        return keyPrefixStrategy.wrapKey(key);
    }

    private ZSetOperations<String, String> ops() {
        return redisTemplate.opsForZSet();
    }

    @Override
    public Boolean add(String key, String value, double score) {
        return Boolean.TRUE.equals(ops().add(wrap(key), value, score));
    }

    @Override
    public Long remove(String key, String... values) {
        return ops().remove(wrap(key), (Object[]) values);
    }

    @Override
    public Double score(String key, String value) {
        return ops().score(wrap(key), value);
    }

    @Override
    public Double incrementScore(String key, String value, double delta) {
        return ops().incrementScore(wrap(key), value, delta);
    }

    @Override
    public Set<String> range(String key, long start, long end) {
        return ops().range(wrap(key), start, end);
    }

    @Override
    public Set<String> reverseRange(String key, long start, long end) {
        return ops().reverseRange(wrap(key), start, end);
    }

    @Override
    public Set<String> rangeByScore(String key, double min, double max) {
        return ops().rangeByScore(wrap(key), min, max);
    }

    @Override
    public Set<String> reverseRangeByScore(String key, double min, double max) {
        return ops().reverseRangeByScore(wrap(key), min, max);
    }

    @Override
    public Long rank(String key, String value) {
        return ops().rank(wrap(key), value);
    }

    @Override
    public Long reverseRank(String key, String value) {
        return ops().reverseRank(wrap(key), value);
    }

    @Override
    public Long size(String key) {
        return ops().zCard(wrap(key));
    }

    @Override
    public Long count(String key, double min, double max) {
        return ops().count(wrap(key), min, max);
    }

    @Override
    public Long removeRange(String key, long start, long end) {
        return ops().removeRange(wrap(key), start, end);
    }

    @Override
    public Long removeRangeByScore(String key, double min, double max) {
        return ops().removeRangeByScore(wrap(key), min, max);
    }
}
