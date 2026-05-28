package com.github.zeng.alt.api.rest;


import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2025年02月25日 16:49
 */
public class TreeRestResponseEntity<T extends Parent<P>, P extends Comparable<P>> extends HttpEntityStatus<TreeEntity<T, P>> {

    protected TreeRestResponseEntity(HttpStatusCode status) {
        this(null, status);
    }

    protected TreeRestResponseEntity(@Nullable TreeEntity<T, P> body, HttpStatusCode status) {
        super(body, status.value());
    }

    protected TreeRestResponseEntity(@Nullable TreeEntity<T, P> body, Integer status) {
        super(body, status);
    }

    public static <T extends Parent<P>, P extends Comparable<P>> TreeRestResponseEntity<T, P> of(TreeEntity<T, P> body) {
        return new TreeRestResponseEntity<>(body, HttpStatus.OK);
    }

    public static <T extends Parent<P>, P extends Comparable<P>> TreeRestResponseEntity<T, P> of(Collection<T> data) {
        if (data == null || data.isEmpty()) {
            return new TreeRestResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Optional<T> first = data.stream().filter(Parent::isRoot).findFirst();
        T rootData = first.orElseThrow(() -> new IllegalArgumentException("根节点不存在"));
        TreeEntity<T, P> root = TreeEntity.of(rootData);
        buildTree(root, data.stream().filter(p -> !p.isRoot()).toList());
        return TreeRestResponseEntity.of(root);
    }

    private static <T extends Parent<P>, P extends Comparable<P>> void buildTree(TreeEntity<T, P> parent, Collection<T> data) {
        if (data == null || data.isEmpty()) {
            return;
        }

        List<T> next = data.stream().filter(p -> !p.parent().equals(parent.getCurrentId())).toList();
        List<TreeEntity<T, P>> children = data.stream()
                .filter(p -> p.parent().equals(parent.getCurrentId()))
                .map(t -> {
                    TreeEntity<T, P> child = TreeEntity.of(t);
                    buildTree(child, next);
                    return child;
                })
                .toList();
        if (!children.isEmpty()) {
            parent.setChildren(children);
        }
    }
}
