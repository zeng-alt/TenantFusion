package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.security.data.DataPermissionDepartmentResolver;
import io.vavr.control.Either;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 从当前租户的有效组织树计算本部门及后代；单次解析不缓存组织成员关系。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Component
@RequiredArgsConstructor
public class JpaDataPermissionDepartmentResolver implements DataPermissionDepartmentResolver {
    private final EntityManager entityManager;

    @Override
    public Either<String, Set<Long>> descendants(String tenant, Long departmentId) {
        final var rows = entityManager.createQuery("""
                select d.deptId as id, p.deptId as parent from Department d
                left join d.parent p on p.tenantBy = :tenant
                where d.tenantBy = :tenant and d.enabled = true
                """, Tuple.class).setParameter("tenant", tenant).getResultList();
        if (rows.stream().noneMatch(row -> departmentId.equals(row.get("id", Long.class)))) {
            return Either.right(Set.of());
        }
        final var children = rows.stream().filter(row -> row.get("parent", Long.class) != null)
                .collect(Collectors.groupingBy(row -> row.get("parent", Long.class),
                        Collectors.mapping(row -> row.get("id", Long.class), Collectors.toSet())));
        final Set<Long> visited = new HashSet<>(Set.of(departmentId));
        List<Long> frontier = List.of(departmentId);
        while (!frontier.isEmpty()) {
            frontier = frontier.stream().flatMap(id -> children.getOrDefault(id, Set.of()).stream())
                    .filter(visited::add).toList();
        }
        return Either.right(Set.copyOf(visited));
    }
}
