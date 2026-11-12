package com.github.zeng.alt.security.data;

import io.vavr.control.Either;

import java.util.Set;

/**
 * 解析租户内的本部门及下级部门，不向其他租户扩展。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface DataPermissionDepartmentResolver {
    Either<String, Set<Long>> descendants(String tenant, Long departmentId);
}
