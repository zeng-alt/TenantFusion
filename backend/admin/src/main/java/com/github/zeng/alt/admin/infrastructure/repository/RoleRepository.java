package com.github.zeng.alt.admin.infrastructure.repository;


import com.github.zeng.alt.admin.infrastructure.entity.Role;
import com.github.zeng.alt.admin.infrastructure.projection.RoleDto;
import com.github.zeng.alt.domain.base.BaseRepository;
import com.github.zeng.alt.rest.annotation.CrudRest;

@CrudRest(path = "/v1/role", listType = RoleDto.class, listAll = true, create = false, patch = false, sort = true)
public interface RoleRepository extends BaseRepository<Role, Long> {

}