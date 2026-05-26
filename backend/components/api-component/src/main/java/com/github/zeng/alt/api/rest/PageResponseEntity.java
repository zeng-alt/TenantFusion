package com.github.zeng.alt.api.rest; 


import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.*;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2025年02月20日 14:58
 */
public class PageResponseEntity<T> extends HttpEntityStatus<PageEntity<Collection<T>>> {

    protected PageResponseEntity(HttpStatusCode status) {
        this(null, status);
    }

    protected PageResponseEntity(@Nullable PageEntity<Collection<T>> body, HttpStatusCode status) {
        super(body, status.value());
    }

    protected PageResponseEntity(Integer status) {
        this(null, status);
    }

    protected PageResponseEntity(@Nullable PageEntity<Collection<T>> body, int rawStatus) {
        this(body, HttpStatusCode.valueOf(rawStatus));
    }

    public static PageResponseEntity<Void> of(int pageSize, int pageNum) {
        PageEntity<Collection<Void>> page = new PageEntity<>();
        page.setPageSize(pageSize);
        page.setPageNum(pageNum);
        page.setData(Collections.emptyList());
        return new PageResponseEntity<>(page, HttpStatus.OK);
    }

    public static <T> PageResponseEntity<T> of(PageEntity<Collection<T>> pageEntity) {
        return new PageResponseEntity<>(pageEntity, HttpStatus.OK);
    }

    public static <T> PageResponseEntity<T> of(T body) {
        return of(new PageEntity<>(List.of(body)));
    }

    public static <T> PageResponseEntity<T> of(Collection<T> data, long totalCount, int pageSize, int pageNum) {
        PageEntity<Collection<T>> page = new PageEntity<>();
        page.setData(data);
        page.setTotal(totalCount);
        page.setPageSize(pageSize);
        page.setPageNum(pageNum);
        return new PageResponseEntity<>(page, HttpStatus.OK);
    }

    public static <T> PageResponseEntity<T> ofNullable(@Nullable PageEntity<Collection<T>> body) {
        if (body == null) {
            return PageResponseEntity.notFound().body(null);
        }
        return PageResponseEntity.ok(body);
    }

    public static <T> PageResponseEntity<T> ofNullable(@Nullable T... body) {
        if (body == null || body.length == 0) {
            return notFound().body(null);
        }
        return PageResponseEntity.ok(Arrays.asList(body));
    }

    public static PageBodyBuilder status(HttpStatusCode status) {
        Assert.notNull(status, "HttpStatusCode must not be null");
        return new PageBuilder(status);
    }

    public static PageBodyBuilder status(Integer status) {
        Assert.notNull(status, "status must not be null");
        return new PageBuilder(status);
    }

    public static PageBodyBuilder ok() {
        return status(HttpStatus.OK);
    }

    public static <T> PageResponseEntity<T> ok(@Nullable PageEntity<Collection<T>> body) {
        return ok().body(body);
    }

    public static <T> PageResponseEntity<T> ok(@Nullable Collection<T> body) {
        return PageResponseEntity.ok(new PageEntity<>(body));
    }

    public static <T> PageResponseEntity<T> ok(@Nullable T body) {
        return ok(List.of(body));
    }

    public static <T> PageResponseEntity<T> of(@Nullable Optional<PageEntity<Collection<T>>> body) {
        Assert.notNull(body, "Body must not be null");
        return body.map(PageResponseEntity::ok).orElseGet(() -> notFound().body(null));
    }

    public static <T> PageResponseEntity<T> ofNullable(@Nullable T body) {
        if (body == null) {
            return notFound().body(null);
        }
        return PageResponseEntity.ok(body);
    }

    public static PageBodyBuilder accepted() {
        return status(HttpStatus.ACCEPTED);
    }

    public static PageBodyBuilder noContent() {
        return status(HttpStatus.NO_CONTENT);
    }

    public static PageBodyBuilder badRequest() {
        return status(HttpStatus.BAD_REQUEST);
    }

    public static PageBodyBuilder notFound() {
        return status(HttpStatus.NOT_FOUND);
    }

    public static PageBodyBuilder unprocessableEntity() {
        return status(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static PageBodyBuilder internalServerError() {
        return status(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static PageBodyBuilder fail() {
        return status(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

