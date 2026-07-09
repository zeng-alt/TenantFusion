package com.github.zeng.alt.api.utils;

import com.github.zeng.alt.api.base.BaseTreeSortReq;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author zengJiaJun
 * @since 2026年05月26日
 * @version 1.0
 */
public final class TreeSortUtils {

    private static void collectIds(
            List<BaseTreeSortReq> nodes,
            Set<Long> ids) {

        for (BaseTreeSortReq node : nodes) {

            ids.add(node.getId());

            if (node.getChildren() != null) {
                collectIds(node.getChildren(), ids);
            }
        }
    }

    public static <T, ID> void update(
            List<BaseTreeSortReq> tree,
            Map<ID, T> entityMap,
            Function<T, ID> idGetter,
            BiConsumer<T, Integer> orderSetter,
            BiConsumer<T, T> parentSetter) {

        walk(tree, null, entityMap,
                idGetter,
                orderSetter,
                parentSetter);
    }

    private static <T, ID> void walk(
            List<BaseTreeSortReq> tree,
            T parent,
            Map<ID, T> entityMap,
            Function<T, ID> idGetter,
            BiConsumer<T, Integer> orderSetter,
            BiConsumer<T, T> parentSetter) {

        int order = 1;

        for (BaseTreeSortReq node : tree) {

            T entity = entityMap.get(node.getId());

            if (entity == null) {
                continue;
            }

            parentSetter.accept(entity, parent);

            orderSetter.accept(entity, order++);

            walk(
                    node.getChildren(),
                    entity,
                    entityMap,
                    idGetter,
                    orderSetter,
                    parentSetter
            );
        }
    }
}