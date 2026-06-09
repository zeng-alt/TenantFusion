package com.github.zeng.alt.storage.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 缓存 Hash 结构操作接口
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public interface CacheHashOperations {

    /**
     * 设置 hash 中的字段值
     *
     * @param key     key
     * @param hashKey hash key
     * @param value   value
     */
    void put(String key, String hashKey, Object value);

    /**
     * 批量设置 hash 中的字段值
     *
     * @param key  key
     * @param map 字段-值映射
     */
    void putAll(String key, Map<String, Object> map);

    /**
     * 获取 hash 中的字段值
     *
     * @param key     key
     * @param hashKey hash key
     * @return value
     */
    <T> T get(String key, String hashKey);

    /**
     * 获取 hash 中所有字段的值
     *
     * @param key key
     * @return hash 所有字段-值映射
     */
    Map<String, Object> entries(String key);

    /**
     * 获取 hash 中所有字段名
     *
     * @param key key
     * @return 字段名集合
     */
    Set<String> keys(String key);

    /**
     * 获取 hash 中所有值
     *
     * @param key key
     * @return 值列表
     */
    List<Object> values(String key);

    /**
     * 获取 hash 中多个字段的值
     *
     * @param key     key
     * @param hashKeys hash keys
     * @return 值列表
     */
    List<Object> multiGet(String key, String... hashKeys);

    /**
     * 删除 hash 中的字段
     *
     * @param key     key
     * @param hashKeys hash keys
     * @return 删除数量
     */
    Long delete(String key, String[] hashKeys);

    /**
     * 判断 hash 中是否存在指定字段
     *
     * @param key     key
     * @param hashKey hash key
     * @return true 存在
     */
    Boolean hasKey(String key, String hashKey);

    /**
     * 获取 hash 大小
     *
     * @param key key
     * @return 大小
     */
    Long size(String key);

    /**
     * 递增 hash 中的字段值
     *
     * @param key     key
     * @param hashKey hash key
     * @param delta   步长
     * @return 递增后的值
     */
    Long increment(String key, String hashKey, long delta);
}
