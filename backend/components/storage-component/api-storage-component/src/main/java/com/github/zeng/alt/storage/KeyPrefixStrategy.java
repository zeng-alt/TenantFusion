package com.github.zeng.alt.storage;

import java.util.Objects;

/**
 * Key 前缀策略接口，用于统一给缓存 key 添加前缀
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
@FunctionalInterface
public interface KeyPrefixStrategy {

    /**
     * 包装 key，添加前缀
     *
     * @param key 原始 key
     * @return 添加前缀后的 key
     */
    String map(String key);

    /**
     * 获取前缀（可选实现）
     *
     * @return 前缀字符串
     */
    default String unmap(String key) {
        return "";
    }

    /**
     * 返回不做任何处理的前缀策略
     */
    static KeyPrefixStrategy noOp() {
        return key -> key;
    }

    /**
     * 根据指定前缀创建策略
     *
     * @param prefix 前缀
     * @return KeyPrefixStrategy
     */
    static KeyPrefixStrategy prefix(String prefix) {



        return new KeyPrefixStrategy() {


            @Override
            public String map(String key) {
                return prefix + ":" + key;
            }

            @Override
            public String unmap(String key) {
                if (key != null && key.startsWith(prefix)) {
                    return key.substring(prefix.length() + 1);
                }
                return key;
            }
        };
    }

    default KeyPrefixStrategy andThen(KeyPrefixStrategy next) {
        Objects.requireNonNull(next);

        return new KeyPrefixStrategy() {
            @Override
            public String map(String key) {
                return next.map(KeyPrefixStrategy.this.map(key));
            }

            @Override
            public String unmap(String key) {
                return KeyPrefixStrategy.this.unmap(next.unmap(key));
            }
        };
    }

}
