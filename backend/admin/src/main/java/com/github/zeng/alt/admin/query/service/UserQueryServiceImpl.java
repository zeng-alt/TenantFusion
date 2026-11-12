package com.github.zeng.alt.admin.query.service;

import app.tozzi.core.JPASearchCore;
import app.tozzi.model.input.JPASearchInput;
import app.tozzi.util.ReflectionUtils;
import com.github.zeng.alt.admin.infrastructure.entity.QUser;
import com.github.zeng.alt.admin.infrastructure.entity.User;
import com.github.zeng.alt.admin.infrastructure.projection.UserDto;
import com.github.zeng.alt.admin.query.api.UserQueryService;
import com.github.zeng.alt.admin.query.api.dto.UserInfoDto;
import com.github.zeng.alt.admin.query.api.dto.UserPageQuery;
import com.github.zeng.alt.admin.query.api.dto.UserSearchQuery;
import com.github.zeng.alt.admin.query.service.transformation.UserQueryMapper;
import com.github.zeng.alt.security.data.jpa.DataPermissionJpaTemplate;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import com.querydsl.core.BooleanBuilder;
import io.vavr.control.Either;
import io.vavr.control.Option;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 用户查询权限在数据库分页、计数和主键读取之前生效。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryServiceImpl implements UserQueryService {
    private static final String LIST_PERMISSION = "GET:/v1/user";
    private static final String DETAIL_PERMISSION = "GET:/v1/user/{id:\\d+}";
    private final DataPermissionJpaTemplate permissions;
    private final UserQueryMapper mapper;
    private final EntityManager entityManager;
    private final UserQueryRoleResolver roles;

    @Override
    public Either<String, Page<UserDto>> page(UserPageQuery query) {
        final var user = QUser.user;
        final var filter = new BooleanBuilder();
        Option.of(query.getUsername()).filter(value -> !value.isBlank())
                .forEach(value -> filter.and(user.username.like("%" + value + "%")));
        Option.of(query.getGender()).filter(value -> !value.isBlank())
                .forEach(value -> filter.and(user.gender.eq(value)));
        Option.of(query.getDeptId()).forEach(value -> filter.and(user.deptId.eq(value)));
        Option.of(query.getEnabled()).forEach(value -> filter.and(user.enabled.eq(value)));
        final String sort = "id".equals(query.getSort()) ? "userId" : query.getSort();
        final var pageable = PageRequest.of(query.getPageNo() - 1, query.getPageSize(),
                Sort.by(Sort.Direction.fromString(query.getOrder()), sort));
        return permissions.query(User.class).permission(LIST_PERMISSION).where(filter)
                .page(pageable).map(this::toPage);
    }

    @Override
    public Either<String, Option<User>> getDetail(Long id) {
        return permissions.query(User.class).permission(DETAIL_PERMISSION).getDetail(id);
    }

    @Override
    public Either<String, Option<UserInfoDto>> getInfo(Long id, String username) {
        final var predicate = id != null ? QUser.user.userId.eq(id) : QUser.user.username.eq(username);
        return permissions.query(User.class).permission("GET:/v1/user/info").where(predicate)
                .list(Sort.unsorted()).map(users -> Option.ofOptional(users.stream().findFirst()).map(user -> {
                    final var dto = mapper.toInfoDto(user);
                    Option.of(user.getDeptId()).forEach(idValue -> entityManager.createQuery("""
                            select d.deptName from Department d where d.tenantBy = :tenant and d.deptId = :id
                            """, String.class).setParameter("tenant", user.getTenantBy())
                            .setParameter("id", idValue).getResultStream().findFirst().ifPresent(dto::setDeptName));
                    return dto;
                }));
    }

    private Page<UserDto> toPage(Page<User> page) {
        roles.resolve(TenantContextHolder.getTenantId(), page.getContent());
        return page.map(mapper::toDto);
    }

    @Override
    public Either<String, Page<UserDto>> search(JPASearchInput input) {
        final var fields = ReflectionUtils.getAllSearchableFields(UserSearchQuery.class);
        final Specification<User> specification = JPASearchCore.specification(input.getFilter(), fields,
                Map.of(), Map.of());
        final var pageable = JPASearchCore.loadSortAndPagination(input.getOptions(), fields, Map.of());
        if (pageable.getPageSize() > 100) {
            return Either.left("每页最多查询 100 条用户");
        }
        return permissions.query(User.class).permission("POST:/v1/user/search").where(specification)
                .page(pageable).map(this::toPage);
    }
}
