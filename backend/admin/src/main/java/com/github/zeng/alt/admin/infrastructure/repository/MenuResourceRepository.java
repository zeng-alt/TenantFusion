package com.github.zeng.alt.admin.infrastructure.repository;

import com.github.zeng.alt.admin.infrastructure.entity.MenuResource;
import com.github.zeng.alt.domain.base.BaseRepository;
import com.github.zeng.alt.rest.annotation.CrudRest;

import java.util.List;

@CrudRest(path = "/menu", sort = true)
public interface MenuResourceRepository extends BaseRepository<MenuResource, Long> {

    List<MenuResource> findByParentIsNull();

    Boolean existsByPath(String path);


}