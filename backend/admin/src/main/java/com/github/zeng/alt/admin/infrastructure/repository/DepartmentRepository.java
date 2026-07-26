package com.github.zeng.alt.admin.infrastructure.repository;

import com.github.zeng.alt.admin.infrastructure.entity.Department;
import com.github.zeng.alt.admin.infrastructure.projection.DeptTreeEntityDto;
import com.github.zeng.alt.domain.base.BaseRepository;
import com.github.zeng.alt.rest.annotation.CrudRest;

import java.util.List;

@CrudRest(path = "/v1/dept", tree = true, treeType = DeptTreeEntityDto.class, sort = true, listAll = true)
public interface DepartmentRepository extends BaseRepository<Department, Long> {

    List<Department> findByParentIsNull();
}
