package com.github.zeng.alt.lock.model;

import java.util.UUID;

/**
 * 分布式锁工具类
 *
 * @author zengJiaJun
 * @since 2026年06月09日
 * @version 1.0
 */
public final class LockUtils {

    private LockUtils() {
    }

    /**
     * 生成简化的 UUID（去除横线）
     *
     * @return UUID 字符串
     */
    public static String simpleUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
