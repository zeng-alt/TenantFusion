package com.github.zeng.alt.api.rest;

import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2025年02月25日 21:51
 */
public class TreeEntity<T extends Parent<P>, P extends Comparable<P>> {
    private final T current;
    private List<TreeRestResponse.TreeNodeResponse<T, P>> children;

    private TreeEntity(T current) {
        this.current = current;
    }

    public P getCurrentId() {
        return current.current();
    }

    public boolean hasNext() {
        return !CollectionUtils.isEmpty(children);
    }
}
