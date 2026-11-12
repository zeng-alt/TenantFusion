package com.github.zeng.alt.security.data;

import io.vavr.control.Either;

import java.util.List;

/**
 * 按租户、资源、权限及有效角色加载数据授权。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface DataPermissionPolicyLoader {
    Either<String, List<DataPermissionRule>> list(String resource, String permission, DataPermissionSubject subject);
}
