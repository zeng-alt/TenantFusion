package com.github.zeng.alt.storage.redisson.suepr;

import com.github.zeng.alt.storage.DefaultKeyPrefixStrategy;
import com.github.zeng.alt.storage.KeyPrefixStrategy;
import org.redisson.api.NameMapper;

import java.util.Objects;

/**
 * redis缓存key前缀处理
 *
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年10月16日 16:41
 */
public class KeyPrefixHandler extends DefaultKeyPrefixStrategy implements KeyPrefixStrategy, NameMapper {


    public KeyPrefixHandler(String keyPrefix) {
        super(keyPrefix);
    }

    @Override
    public KeyPrefixHandler andThen(KeyPrefixStrategy next) {
        Objects.requireNonNull(next);
        return new KeyPrefixHandler(next.map(""));
    }
}