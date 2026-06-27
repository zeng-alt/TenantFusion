package com.github.zeng.alt.admin.command.infrastructure.repository;

import com.github.zeng.alt.admin.command.infrastructure.entity.MenuResource;
import com.github.zeng.alt.domain.base.BaseRepository;
import com.github.zeng.alt.rest.annotation.CrudRest;

import java.util.List;

@CrudRest(path = "/menu")
public interface MenuResourceRepository extends BaseRepository<MenuResource, Long> {

    // 根节点菜单（过滤已由全局处理：tenant, enabled, show 可能需要在应用层判断）
    List<MenuResource> findByParentIsNullOrderByOrderAsc();

    List<MenuResource> findByParentIdOrderByOrderAsc(Long parentId);
}