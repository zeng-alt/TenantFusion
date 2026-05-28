package com.github.zeng.alt.domain.base;

import com.github.zeng.alt.api.base.BaseSortReq;
import com.github.zeng.alt.api.base.BaseTreeSortReq;
import com.github.zeng.alt.api.rest.PageRestResponse;
import io.vavr.control.Option;

import java.util.List;

/**
 * @author zengJiaJun
 * @crateTime 2025年12月15日 13:51
 * @version 1.0
 */
public interface IService<T extends BaseEntity<Long>> {


    /**
     * 获取对应 entity 的 BaseMapper
     *
     * @return BaseMapper
     */
    BaseRepository<T, Long> getRepository();

    /**
     * 获取 entity 的 class
     *
     * @return {@link Class<T>}
     */
    Class<T> getEntityClass();

    Option<T> getById(Long id);

    Long save(T entity);

    List<T> list();

    Iterable<T> list(T t);

    Long update(T entity);

    void deleteById(Long id);

    void deleteByIds(List<Long> ids);

    <P extends BasePage> PageRestResponse<T> listForPage(P page);

    <P extends BasePage> PageRestResponse<T> listForPage(P page, T entity);

    void sort(List<BaseSortReq> req);

    void swapSort(List<BaseSortReq> req);

    void sortTree(List<BaseTreeSortReq> req);

    T updateAll(T entity);
}
