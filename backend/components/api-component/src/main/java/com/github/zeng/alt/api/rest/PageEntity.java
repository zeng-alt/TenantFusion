package com.github.zeng.alt.api.rest;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zengJiaJun
 * @version 1.0
 * @since 2025年02月20日 15:01
 */
@Data
public class PageEntity<T> {

    private Integer pageNum = 1;
    private Integer pageSize = 0;
    private Long total = 0L;
    private List<T> pageData = new ArrayList<>();


    public PageEntity() {
    }

    public PageEntity(List<T> pageData) {
        this.pageData = pageData;
        this.total = (long) pageData.size();
    }

    /**
     * @return 当前页第一条数据的偏移量（offset）
     */
    public Integer getFirstNum() {
        return (getPageNum() - 1) * getPageSize();
    }

    /**
     * @return 总页数
     */
    public long getTotalPages() {
        return getTotal() % getPageSize() == 0 ? getTotal() / getPageSize() : (getTotal() / getPageSize()) + 1;
    }
}
