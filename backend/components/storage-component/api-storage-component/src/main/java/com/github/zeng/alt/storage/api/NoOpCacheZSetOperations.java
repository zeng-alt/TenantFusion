package com.github.zeng.alt.storage.api;

import java.util.Collections;
import java.util.Set;

/**
 * ZSet 结构空实现，所有操作不执行任何实际逻辑
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public class NoOpCacheZSetOperations implements CacheZSetOperations {

    @Override
    public Boolean add(String key, String value, double score) {
        return false;
    }

    @Override
    public boolean remove(String key, String... values) {
        return false;
    }

    @Override
    public Double score(String key, String value) {
        return null;
    }

    @Override
    public Double incrementScore(String key, String value, double delta) {
        return 0.0;
    }

    @Override
    public Set<String> range(String key, long start, long end) {
        return Collections.emptySet();
    }

    @Override
    public Set<String> reverseRange(String key, long start, long end) {
        return Collections.emptySet();
    }

    @Override
    public Set<String> rangeByScore(String key, double min, double max) {
        return Collections.emptySet();
    }

    @Override
    public Set<String> reverseRangeByScore(String key, double min, double max) {
        return Collections.emptySet();
    }

    @Override
    public Long rank(String key, String value) {
        return null;
    }

    @Override
    public Long reverseRank(String key, String value) {
        return null;
    }

    @Override
    public Long size(String key) {
        return 0L;
    }

    @Override
    public Long count(String key, double min, double max) {
        return 0L;
    }

    @Override
    public Long removeRange(String key, long start, long end) {
        return 0L;
    }

    @Override
    public Long removeRangeByScore(String key, double min, double max) {
        return 0L;
    }
}
