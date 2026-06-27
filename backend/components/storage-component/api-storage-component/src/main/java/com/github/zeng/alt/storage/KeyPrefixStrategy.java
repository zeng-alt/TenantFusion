package com.github.zeng.alt.storage;

import java.util.List;
import java.util.Objects;

@FunctionalInterface
public interface KeyPrefixStrategy {

    String map(String key);

    default String unmap(String key) {
        return key;
    }

    static KeyPrefixStrategy identity() {
        return key -> key;
    }

    static KeyPrefixStrategy of(String prefix) {

        String normalized =
                prefix.endsWith(":")
                        ? prefix
                        : prefix + ":";

        return new KeyPrefixStrategy() {

            @Override
            public String map(String key) {
                return normalized + key;
            }

            @Override
            public String unmap(String key) {

                if (key != null &&
                        key.startsWith(normalized)) {

                    return key.substring(normalized.length());
                }

                return key;
            }
        };
    }

    default KeyPrefixStrategy andThen(
            KeyPrefixStrategy next) {

        Objects.requireNonNull(next);

        return new CompositeKeyPrefixStrategy(
                List.of(this, next));
    }
}