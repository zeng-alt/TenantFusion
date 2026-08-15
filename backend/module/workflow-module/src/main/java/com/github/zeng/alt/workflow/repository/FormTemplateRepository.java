package com.github.zeng.alt.workflow.repository;

import com.github.zeng.alt.domain.base.BaseRepository;
import com.github.zeng.alt.workflow.entity.FormTemplateEntity;

import java.util.Optional;

/**
 * 动态表单模板 Repository
 *
 * @author zengAlt
 */
public interface FormTemplateRepository extends BaseRepository<FormTemplateEntity, Long> {

    /**
     * 判断模板编码是否已存在（用于创建时校验唯一性）
     *
     * @param code 模板编码
     * @return 是否已存在
     */
    boolean existsByCode(String code);

    /**
     * 按模板编码查询（编码全局唯一）
     *
     * @param code 模板编码
     * @return 模板
     */
    Optional<FormTemplateEntity> findByCode(String code);
}