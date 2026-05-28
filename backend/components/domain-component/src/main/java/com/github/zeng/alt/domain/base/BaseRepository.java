package com.github.zeng.alt.domain.base;

import io.vavr.control.Option;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.QueryByExampleExecutor;

import java.util.List;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年08月26日 20:16
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends Repository<T, ID>, QuerydslPredicateExecutor<T>, QueryByExampleExecutor<T> {

    <S extends T> S save(S entity);

    void saveAll(Iterable<T> entities);

    Option<T> findById(ID id);

    List<T> findByIdIn(Iterable<ID> ids);

    void deleteById(ID id);

    void delete(T entities);

    <S extends T> List<S> findAll();

    void deleteAllById(Iterable<? extends ID> ids);

    Page<T> findAll(Pageable pageable);
}
