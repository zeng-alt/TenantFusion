package com.github.zeng.alt.storage.redisson.suepr;

import com.github.zeng.alt.storage.CacheZSetOperations;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;

import java.util.*;

/**
 * Redisson ZSet（有序集合）结构操作实现
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public class RedissonZSetOperations implements CacheZSetOperations {

    private final RedissonClient redissonClient;

    public RedissonZSetOperations(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public Boolean add(String key, String value, double score) {
        return redissonClient.getScoredSortedSet(key).add(score, value);
    }

    @Override
    public boolean remove(String key, String... values) {
        return redissonClient.getScoredSortedSet(key).removeAll(Arrays.asList(values));
    }

    @Override
    public Double score(String key, String value) {
        return redissonClient.getScoredSortedSet(key).getScore(value);
    }

    @Override
    public Double incrementScore(String key, String value, double delta) {
        return redissonClient.getScoredSortedSet(key).addScore(value, delta);
    }

    @Override
    public Set<String> range(String key, long start, long end) {
        RScoredSortedSet<String> scoredSortedSet = redissonClient.getScoredSortedSet(key);
        Collection<String> values = scoredSortedSet.valueRange((int) start, (int) end);
        return new LinkedHashSet<>(values);
    }

    @Override
    public Set<String> reverseRange(String key, long start, long end) {
        RScoredSortedSet<String> scoredSortedSet = redissonClient.getScoredSortedSet(key);
        Collection<String> values = scoredSortedSet.valueRangeReversed((int) start, (int) end);
        return new LinkedHashSet<>(values);
    }

    @Override
    public Set<String> rangeByScore(String key, double min, double max) {
        RScoredSortedSet<String> scoredSortedSet = redissonClient.getScoredSortedSet(key);
        Collection<String> values = scoredSortedSet.valueRange(min, true, max, true);
        return new LinkedHashSet<>(values);
    }

    @Override
    public Set<String> reverseRangeByScore(String key, double min, double max) {
        RScoredSortedSet<String> set = redissonClient.getScoredSortedSet(key);
        Collection<String> values = set.valueRange(min, true, max, true);
        // 按分数从高到低排列
        return new LinkedHashSet<>(new ArrayList<>(values).reversed());
    }

    @Override
    public Long rank(String key, String value) {
        Integer rank = redissonClient.getScoredSortedSet(key).rank(value);
        return rank != null ? rank.longValue() : null;
    }

    @Override
    public Long reverseRank(String key, String value) {
        Integer rank = redissonClient.getScoredSortedSet(key).revRank(value);
        return rank != null ? rank.longValue() : null;
    }

    @Override
    public Long size(String key) {
        return (long) redissonClient.getScoredSortedSet(key).size();
    }

    @Override
    public Long count(String key, double min, double max) {
        return (long) redissonClient.getScoredSortedSet(key).count(min, true, max, true);
    }

    @Override
    public Long removeRange(String key, long start, long end) {
        return (long) redissonClient.getScoredSortedSet(key).removeRangeByRank((int) start, (int) end);
    }

    @Override
    public Long removeRangeByScore(String key, double min, double max) {
        return (long) redissonClient.getScoredSortedSet(key).removeRangeByScore(min, true, max, true);
    }
}
