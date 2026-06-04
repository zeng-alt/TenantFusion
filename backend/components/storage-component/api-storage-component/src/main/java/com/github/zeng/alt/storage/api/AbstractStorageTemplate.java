package com.github.zeng.alt.storage.api;

/**
 * 抽象 StorageTemplate 基类，提供 key 前缀包装能力
 *
 * @author zengJiaJun
 * @since 2026年06月04日
 * @version 1.0
 */
public abstract class AbstractStorageTemplate implements StorageTemplate {

    private final KeyPrefixStrategy keyPrefixStrategy;

    protected AbstractStorageTemplate(KeyPrefixStrategy keyPrefixStrategy) {
        this.keyPrefixStrategy = keyPrefixStrategy;
    }

    /**
     * 包装 key，添加前缀
     *
     * @param key 原始 key
     * @return 添加前缀后的 key
     */
    protected String wrapKey(String key) {
        return keyPrefixStrategy.wrapKey(key);
    }

    /**
     * 获取当前前缀策略
     *
     * @return KeyPrefixStrategy
     */
    protected KeyPrefixStrategy getKeyPrefixStrategy() {
        return keyPrefixStrategy;
    }
}
