package com.github.zeng.alt.storage;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 缓存 List 结构操作接口
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public interface CacheListOperations {

    /**
     * 从左边推入元素
     *
     * @param key   key
     * @param value value
     * @return 列表长度
     */
    Long leftPush(String key, String value);

    /**
     * 从左边批量推入元素
     *
     * @param key    key
     * @param values values
     * @return 列表长度
     */
    Long leftPushAll(String key, String... values);

    /**
     * 从右边推入元素
     *
     * @param key   key
     * @param value value
     * @return 列表长度
     */
    Long rightPush(String key, String value);

    /**
     * 从右边批量推入元素
     *
     * @param key    key
     * @param values values
     * @return 列表长度
     */
    Long rightPushAll(String key, String... values);

    /**
     * 从左边弹出元素
     *
     * @param key key
     * @return 元素
     */
    String leftPop(String key);

    /**
     * 从左边弹出元素（带超时阻塞）
     *
     * @param key     key
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 元素
     */
    String leftPop(String key, long timeout, TimeUnit unit);

    /**
     * 从右边弹出元素
     *
     * @param key key
     * @return 元素
     */
    String rightPop(String key);

    /**
     * 从右边弹出元素（带超时阻塞）
     *
     * @param key     key
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 元素
     */
    String rightPop(String key, long timeout, TimeUnit unit);

    /**
     * 获取列表指定范围的元素
     *
     * @param key   key
     * @param start 起始索引
     * @param end   结束索引
     * @return 元素列表
     */
    List<String> range(String key, long start, long end);

    /**
     * 获取列表长度
     *
     * @param key key
     * @return 长度
     */
    Long size(String key);

    /**
     * 获取指定索引的元素
     *
     * @param key   key
     * @param index 索引
     * @return 元素
     */
    String index(String key, long index);

    /**
     * 设置指定索引的元素
     *
     * @param key   key
     * @param index 索引
     * @param value value
     */
    void set(String key, long index, String value);

    /**
     * 移除指定数量的元素
     *
     * @param key   key
     * @param count 移除数量（>0 从左向右，<0 从右向左，=0 移除所有）
     * @param value value
     * @return 移除数量
     */
    Long remove(String key, long count, String value);

    /**
     * 裁剪列表
     *
     * @param key   key
     * @param start 起始索引
     * @param end   结束索引
     */
    void trim(String key, long start, long end);
}
