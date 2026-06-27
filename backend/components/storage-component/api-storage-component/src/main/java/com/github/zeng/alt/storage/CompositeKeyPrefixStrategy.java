package com.github.zeng.alt.storage;

import java.util.List;
import java.util.ListIterator;

public final class CompositeKeyPrefixStrategy
        implements KeyPrefixStrategy {

    private final List<KeyPrefixStrategy> delegates;

    public CompositeKeyPrefixStrategy(
            List<KeyPrefixStrategy> delegates) {

        this.delegates = List.copyOf(delegates);
    }

    @Override
    public String map(String key) {

        String result = key;

        for (KeyPrefixStrategy strategy : delegates) {
            result = strategy.map(result);
        }

        return result;
    }

    @Override
    public String unmap(String key) {

        String result = key;

        ListIterator<KeyPrefixStrategy> iterator =
                delegates.listIterator(delegates.size());

        while (iterator.hasPrevious()) {
            result = iterator.previous().unmap(result);
        }

        return result;
    }
}