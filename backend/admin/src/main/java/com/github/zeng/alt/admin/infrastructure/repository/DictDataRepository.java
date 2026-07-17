package com.github.zeng.alt.admin.infrastructure.repository;

import com.github.zeng.alt.admin.infrastructure.entity.DictData;
import com.github.zeng.alt.domain.base.BaseRepository;
import com.github.zeng.alt.rest.annotation.CrudRest;

/**
 * @author zengJiaJun
 * @since 2026年07月16日
 * @version 1.0
 */
@CrudRest(path = "/v1/dict/data", listAll = true, sort = true)
public interface DictDataRepository extends BaseRepository<DictData, Long> {
}

