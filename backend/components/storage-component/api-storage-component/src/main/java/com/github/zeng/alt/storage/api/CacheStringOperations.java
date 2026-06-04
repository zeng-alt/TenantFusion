package com.github.zeng.alt.storage.api;

import java.util.concurrent.TimeUnit;

/**
 * 缓存 String 结构操作接口
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public interface CacheStringOperations {

    /**
     * 设置缓存
     *
     * @param key   key
     * @param value value
     */
    void set(String key, String value);

    /**
     * 设置缓存（带过期时间）
     *
     * @param key     key
     * @param value   value
     * @param timeout 超时时间
     * @param unit    时间单位
     */
    void set(String key, String value, long timeout, TimeUnit unit);

    /**
     * 获取缓存
     *
     * @param key key
     * @return value
     */
    String get(String key);

    /**
     * 当 key 不存在时设置缓存
     *
     * @param key   key
     * @param value value
     * @return true 设置成功，false key 已存在
     */
    Boolean setIfAbsent(String key, String value);

    /**
     * 当 key 不存在时设置缓存（带过期时间）
     *
     * @param key     key
     * @param value   value
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return true 设置成功，false key 已存在
     */
    Boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit);

    /**
     * 设置过期时间
     *
     * @param key     key
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return true 设置成功
     */
    Boolean expire(String key, long timeout, TimeUnit unit);

    /**
     * 获取过期时间
     *
     * @param key key
     * @return 过期时间（秒），-1 表示永不过期，-2 表示不存在
     */
    Long getExpire(String key);

    /**
     * 删除缓存
     *
     * @param key key
     * @return true 删除成功
     */
    Boolean delete(String key);

    /**
     * 判断 key 是否存在
     *
     * @param key key
     * @return true 存在
     */
    Boolean hasKey(String key);

    /**
     * 递增
     *
     * @param key key
     * @return 递增后的值
     */
    Long increment(String key);

    /**
     * 递增指定步长
     *
     * @param key   key
     * @param delta 步长
     * @return 递增后的值
     */
    Long increment(String key, long delta);
}
