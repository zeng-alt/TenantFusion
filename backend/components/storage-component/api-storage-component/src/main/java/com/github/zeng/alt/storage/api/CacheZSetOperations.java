package com.github.zeng.alt.storage.api;

import java.util.Set;

/**
 * 缓存 ZSet（有序集合）结构操作接口
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public interface CacheZSetOperations {

    /**
     * 添加元素到有序集合
     *
     * @param key   key
     * @param value value
     * @param score 分数
     * @return true 添加成功
     */
    Boolean add(String key, String value, double score);

    /**
     * 移除有序集合中的元素
     *
     * @param key    key
     * @param values values
     * @return 移除数量
     */
    Long remove(String key, String... values);

    /**
     * 获取元素的分数
     *
     * @param key   key
     * @param value value
     * @return 分数
     */
    Double score(String key, String value);

    /**
     * 递增元素的分数
     *
     * @param key   key
     * @param value value
     * @param delta 增量
     * @return 递增后的分数
     */
    Double incrementScore(String key, String value, double delta);

    /**
     * 获取有序集合中指定排名范围的元素（按分数从低到高）
     *
     * @param key   key
     * @param start 起始排名
     * @param end   结束排名
     * @return 元素集合
     */
    Set<String> range(String key, long start, long end);

    /**
     * 获取有序集合中指定排名范围的元素（按分数从高到低）
     *
     * @param key   key
     * @param start 起始排名
     * @param end   结束排名
     * @return 元素集合
     */
    Set<String> reverseRange(String key, long start, long end);

    /**
     * 获取有序集合中指定分数范围的元素
     *
     * @param key key
     * @param min 最小分数
     * @param max 最大分数
     * @return 元素集合
     */
    Set<String> rangeByScore(String key, double min, double max);

    /**
     * 获取有序集合中指定分数范围的元素（按分数从高到低）
     *
     * @param key key
     * @param min 最小分数
     * @param max 最大分数
     * @return 元素集合
     */
    Set<String> reverseRangeByScore(String key, double min, double max);

    /**
     * 获取元素的排名（从 0 开始，按分数从低到高）
     *
     * @param key   key
     * @param value value
     * @return 排名
     */
    Long rank(String key, String value);

    /**
     * 获取元素的排名（从 0 开始，按分数从高到低）
     *
     * @param key   key
     * @param value value
     * @return 排名
     */
    Long reverseRank(String key, String value);

    /**
     * 获取有序集合大小
     *
     * @param key key
     * @return 大小
     */
    Long size(String key);

    /**
     * 获取有序集合中指定分数范围内的元素数量
     *
     * @param key key
     * @param min 最小分数
     * @param max 最大分数
     * @return 数量
     */
    Long count(String key, double min, double max);

    /**
     * 移除有序集合中指定排名范围的元素
     *
     * @param key   key
     * @param start 起始排名
     * @param end   结束排名
     * @return 移除数量
     */
    Long removeRange(String key, long start, long end);

    /**
     * 移除有序集合中指定分数范围的元素
     *
     * @param key key
     * @param min 最小分数
     * @param max 最大分数
     * @return 移除数量
     */
    Long removeRangeByScore(String key, double min, double max);
}
