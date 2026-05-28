package com.github.zeng.alt.domain.base;


import com.github.zeng.alt.api.base.BaseSortReq;
import com.github.zeng.alt.api.base.BaseTreeSortReq;
import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.bean.BeanHelper;
import io.vavr.control.Option;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ResolvableType;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author zengJiaJun
 * @crateTime 2025年12月15日 13:52
 * @version 1.0
 */
public abstract class ServiceImpl<M extends BaseRepository<T, Long>, T extends BaseEntity<Long>> implements IService<T> {

    protected Log log = LogFactory.getLog(getClass());

    @Autowired
    protected M repository;

    @Autowired
    @PersistenceContext
    protected EntityManager em;

    @Override
    public M getRepository() {
        return repository;
    }

    protected Class<T> entityClass = currentModelClass();

    @Override
    public Class<T> getEntityClass() {
        return entityClass;
    }

    protected Class<T> mapperClass = currentMapperClass();

    protected Class<T> currentMapperClass() {
        return (Class<T>) this.getResolvableType().as(ServiceImpl.class).getGeneric(0).getType();
    }

    protected Class<T> currentModelClass() {
        return (Class<T>) this.getResolvableType().as(ServiceImpl.class).getGeneric(1).getType();
    }

    /**
     * @see ResolvableType
     * @since 3.4.3
     */
    protected ResolvableType getResolvableType() {
        return ResolvableType.forClass(ClassUtils.getUserClass(getClass()));
    }

    @Override
    public Option<T> getById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Long save(T entity) {
        return repository.save(entity).getId();
    }

    @Override
    public List<T> list() {
        return repository.findAll();
    }

    @Override
    public Iterable<T> list(T t) {
        ExampleMatcher matcher = ExampleMatcher.matchingAll()
                .withIgnoreNullValues()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING);
        Example<T> example = Example.of(t, matcher);
        return repository.findAll(example);
    }

    @Override
    public Long update(T entity) {
        return repository
                .findById(entity.getId())
                .map(e -> {
                    BeanHelper.copyPropertiesIgnoringNull(entity, e);
                    return repository.save(e);
                })
                .map(T::getId)
                .getOrNull();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public T updateAll(T entity) {
        return repository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        if (id == null) return;
        this.deleteByIds(List.of(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        repository.deleteAllById(ids);
    }


    @Override
    public <P extends BasePage> PageRestResponse<T> listForPage(P page) {
        ExampleMatcher matcher = ExampleMatcher.matchingAll()
                .withIgnoreNullValues()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING);
        Example<T> example = Example.of(BeanHelper.copyToObject(page, entityClass), matcher);
        Page<T> all = repository.findAll(example, page.toPage());
        return PageRestResponse.of(all.getContent(), all.getTotalElements(), page.getPageSize(), page.getPage());
    }

    @Override
    public <P extends BasePage> PageRestResponse<T> listForPage(P page, T entity) {
        repository.findAll();
        ExampleMatcher matcher = ExampleMatcher.matchingAll()
                .withIgnoreNullValues()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING);
        Example<T> example = Example.of(entity, matcher);
        Page<T> all = repository.findAll(example, page.toPage());
        return PageRestResponse.of(all.getContent(), all.getTotalElements(), page.getPageSize(), page.getPage());
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sort(List<BaseSortReq> req) {
        if (CollectionUtils.isEmpty(req)) return;

        String entityName = entityClass.getSimpleName();

        for (int i = 0; i < req.size(); i++) {
            BaseSortReq dto = req.get(i);

            em.createQuery(
                        """
                        update %s e
                        set e.sort = :sort
                        where e.id = :id
                        """.formatted(entityName)
                    )
                    .setParameter("sort", dto.getSort())
                    .setParameter("id", dto.getId())
                    .executeUpdate();
            if (i % 50 == 0) {
                em.flush();
                em.clear();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void swapSort(List<BaseSortReq> req) {
        if (CollectionUtils.isEmpty(req)) return;
        String entityName = entityClass.getSimpleName();
        for (int i = 0; i < req.size(); i++) {
            em.createQuery(
                            """
                            update %s e
                            set e.sort = :newSort
                            where e.id = :id
                            """.formatted(entityName)
                    )
                    .setParameter("newSort", req.get(i).getSort())
                    .setParameter("id", req.get(i).getId())
                    .executeUpdate();
            if (i % 50 == 0) {
                em.flush();
                em.clear();
            }
        }


    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sortTree(List<BaseTreeSortReq> req) {
        if (CollectionUtils.isEmpty(req)) return;

        String entityName = entityClass.getSimpleName();

        // 顶层节点列表，不更新 parentId
        for (int i = 0; i < req.size(); i++) {
            BaseTreeSortReq node = req.get(i);

            // 只更新 sort
            em.createQuery(
                            """
                            update %s e
                            set e.sort = :sort
                            where e.id = :id
                            """.formatted(entityName)
                    )
                    .setParameter("sort", node.getSort())
                    .setParameter("id", node.getId())
                    .executeUpdate();

            if (i % 50 == 0) {
                em.flush();
                em.clear();
            }

            // 递归更新子节点，传当前节点ID作为 parentId
            updateChildren(node.getChildren(), entityName, node.getId());
        }
    }

    /**
     * 递归更新子节点的 sort 和 parentId
     */
    private void updateChildren(List<BaseTreeSortReq> children, String entityName, Long parentId) {
        if (CollectionUtils.isEmpty(children)) return;

        for (int i = 0; i < children.size(); i++) {
            BaseTreeSortReq node = children.get(i);

            // 更新 sort 和 parentId
            em.createQuery(
                            """
                            update %s e
                            set e.sort = :sort,
                            e.parent.id = :parentId
                            where e.id = :id
                            """.formatted(entityName)
                    )
                    .setParameter("sort", node.getSort())
                    .setParameter("parentId", parentId)
                    .setParameter("id", node.getId())
                    .executeUpdate();

            if (i % 50 == 0) {
                em.flush();
                em.clear();
            }

            // 递归处理更深层子节点
            updateChildren(node.getChildren(), entityName, node.getId());
        }
    }


}
