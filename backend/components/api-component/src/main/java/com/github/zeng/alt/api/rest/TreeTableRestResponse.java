package com.github.zeng.alt.api.rest;


import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年10月22日 21:41
 */
public class TreeTableRestResponse<T extends Parent<P>, P extends Comparable<P>> extends RestResponse<Collection<T>> {

    protected TreeTableRestResponse(Collection<T> data) {
        super(RestResponse.SUCCESS_CODE, "success");
        setData(buildTable(data));
    }

    /**
     * 将树形数据转换为扁平表格数据（根节点在前，子节点按层级排列）
     */
    private List<T> buildTable(Collection<T> data) {
        List<T> result = new LinkedList<>();
        if (data == null || data.isEmpty()) {
            return result;
        }

        Optional<T> first = data.stream().filter(Parent::isRoot).findFirst();
        T root = first.orElseThrow(() -> new IllegalArgumentException("根节点不存在"));

        // 收集所有节点的映射
        Map<P, List<T>> childrenMap = data.stream()
                .filter(p -> !p.isRoot())
                .collect(Collectors.groupingBy(
                        Parent::parent,
                        HashMap::new,
                        Collectors.toCollection(LinkedList::new)
                ));

        // BFS 层级遍历
        Queue<T> queue = new LinkedList<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            T current = queue.poll();
            result.add(current);
            List<T> children = childrenMap.getOrDefault(current.current(), Collections.emptyList());
            queue.addAll(children);
        }

        return result;
    }

    public static <T extends Parent<P>, P extends Comparable<P>> TreeTableRestResponse<T, P> apply(Collection<T> data) {
        return new TreeTableRestResponse<>(data);
    }

}
