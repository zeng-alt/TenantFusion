package com.github.zeng.alt.api.rest;

import com.zjj.autoconfigure.component.core.Response;
import com.zjj.autoconfigure.component.core.ResponseEnum;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年10月22日 21:41
 */
public class TreeTableRestResponse<T extends Parent<P>, P extends Comparable<P>> extends RestResponse<Collection<T>> {

    protected TreeTableRestResponse(Collection<T> data) {
        super(ResponseEnum.SUCCESS.getCode(), ResponseEnum.SUCCESS.getMessage());
        setData(data);
    }

    public static <T extends Parent<P>, P extends Comparable<P>> TreeTableRestResponse<T, P> apply(Collection<T> data) {
        return new TreeTableRestResponse<>(data);
    }


    @Override
    protected RestResponse<Collection<T>> setData(Collection<T> data) {
        List<T> result = new LinkedList<>();
        if (data != null) {

            Optional<T> first = data.stream().filter(Parent::isRoot).findFirst();
            T root = first.orElseThrow(() -> new IllegalArgumentException("根节点不存在"));
            result.add(root);

        }
        return super.setData(result);
    }

}
