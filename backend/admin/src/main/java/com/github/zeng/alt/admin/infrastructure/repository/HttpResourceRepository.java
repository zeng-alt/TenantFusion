package com.github.zeng.alt.admin.infrastructure.repository;

import com.github.zeng.alt.admin.infrastructure.entity.HttpResource;
import com.github.zeng.alt.domain.base.BaseRepository;
import com.github.zeng.alt.rest.annotation.CrudRest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

//@CrudRest(path = "/http")
@Repository
public interface HttpResourceRepository extends BaseRepository<HttpResource, Long> {

    Optional<HttpResource> findByPathAndMethod(String path, String method);

//    List<HttpResource> findByMenuId(Long menuId);

    // 根据路径前缀查找（用于通配符匹配时可自定义）
    List<HttpResource> findByPathStartingWith(String pathPrefix);
}