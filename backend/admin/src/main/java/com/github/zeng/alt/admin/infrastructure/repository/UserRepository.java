package com.github.zeng.alt.admin.infrastructure.repository;

import com.github.zeng.alt.admin.infrastructure.entity.User;
import com.github.zeng.alt.admin.infrastructure.projection.UserDto;
import com.github.zeng.alt.domain.base.BaseRepository;
import com.github.zeng.alt.rest.annotation.CrudRest;
import io.vavr.control.Option;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

@CrudRest(path = "/v1/user", listType = UserDto.class, create = false, update = false, patch = false, list = false, detail = false)
public interface UserRepository extends BaseRepository<User, Long> {

    /** 认证时预取角色，原生环境不生成 Hibernate 运行时关联代理。 */
    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Optional<User> findByUsername(String username);

    /** 单条主体及当前用户资料读取使用同一预取策略。 */
    @Override
    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Option<User> findById(Long id);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // 列表查询已自动过滤 is_deleted = false, is_enabled = true, tenant_by = 当前租户
    List<User> findAllByOrderByCreatedDateDesc();
}
