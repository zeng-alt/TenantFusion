package com.github.zeng.alt.storage;

import org.springframework.util.StringUtils;

/**
 * @author zengJiaJun
 * @since 2026年06月18日
 * @version 1.0
 */
public class DefaultKeyPrefixStrategy implements KeyPrefixStrategy {

    private final String keyPrefix;

    public DefaultKeyPrefixStrategy(String keyPrefix) {
        //前缀为空 则返回空前缀
        this.keyPrefix = !StringUtils.hasText(keyPrefix) ? "" : keyPrefix + ":";
    }

    /**
     * 增加前缀
     */
    @Override
    public String map(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        if (StringUtils.hasText(keyPrefix) && !name.startsWith(keyPrefix)) {
            return keyPrefix + name;
        }
        return name;
    }

    /**
     * 去除前缀
     */
    @Override
    public String unmap(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        if (StringUtils.hasText(keyPrefix) && name.startsWith(keyPrefix)) {
            return name.substring(keyPrefix.length());
        }
        return name;
    }
}
