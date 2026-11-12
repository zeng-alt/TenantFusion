package com.github.zeng.alt.security.data;

import io.vavr.control.Either;

/**
 * 校验身份、基础查询权限和有效角色，并加载可信用户属性。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface DataPermissionSubjectResolver {
    Either<String, DataPermissionSubject> resolve(String permission);
}
