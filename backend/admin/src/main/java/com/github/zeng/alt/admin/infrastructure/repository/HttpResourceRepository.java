package com.github.zeng.alt.admin.infrastructure.repository;

import com.github.zeng.alt.admin.infrastructure.entity.HttpResource;
import com.github.zeng.alt.domain.base.BaseRepository;
import com.github.zeng.alt.rest.annotation.CrudRest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@CrudRest(
        path = "/v1/http"
)
@Repository
public interface HttpResourceRepository extends BaseRepository<HttpResource, Long> {

    Optional<HttpResource> findByPathAndMethod(String path, String method);

    org.springframework.data.domain.Page<HttpResource> findByMenuId(Long menuId, org.springframework.data.domain.Pageable pageable);

    List<HttpResource> findByPathStartingWith(String pathPrefix);
}