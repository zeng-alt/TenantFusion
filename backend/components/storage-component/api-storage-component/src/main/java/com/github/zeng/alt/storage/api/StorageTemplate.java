package com.github.zeng.alt.storage.api;

/**
 * 缓存操作模板，聚合了 String、List、Hash、ZSet 四种数据结构操作
 * 通过 opsForXxx() 获取对应操作接口
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public interface StorageTemplate {

    /**
     * 获取 String 结构操作
     *
     * @return CacheStringOperations
     */
    CacheStringOperations opsForString();

    /**
     * 获取 List 结构操作
     *
     * @return CacheListOperations
     */
    CacheListOperations opsForList();

    /**
     * 获取 Hash 结构操作
     *
     * @return CacheHashOperations
     */
    CacheHashOperations opsForHash();

    /**
     * 获取 ZSet 结构操作
     *
     * @return CacheZSetOperations
     */
    CacheZSetOperations opsForZSet();

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
     * 设置过期时间
     *
     * @param key     key
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return true 设置成功
     */
    Boolean expire(String key, long timeout, java.util.concurrent.TimeUnit unit);

    /**
     * 获取过期时间
     *
     * @param key key
     * @return 过期时间（秒），-1 表示永不过期，-2 表示不存在
     */
    Long getExpire(String key);
}
