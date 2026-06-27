package com.github.zeng.alt.storage;

import org.springframework.core.Ordered;

public interface KeyPrefixContributor
        extends Ordered {

    KeyPrefixStrategy strategy();
}