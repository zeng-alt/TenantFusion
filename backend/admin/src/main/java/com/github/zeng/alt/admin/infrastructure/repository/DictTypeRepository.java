package com.github.zeng.alt.admin.infrastructure.repository;

import com.github.zeng.alt.admin.infrastructure.entity.DictType;
import com.github.zeng.alt.domain.base.BaseRepository;
import com.github.zeng.alt.rest.annotation.CrudRest;

/**
 * @author zengJiaJun
 * @since 2026年07月16日
 * @version 1.0
 */
@CrudRest(path = "/v1/dict/type")
public interface DictTypeRepository extends BaseRepository<DictType, Long> {
}

