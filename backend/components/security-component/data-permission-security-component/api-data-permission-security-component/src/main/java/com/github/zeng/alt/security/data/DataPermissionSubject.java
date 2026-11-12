package com.github.zeng.alt.security.data;

import io.vavr.control.Option;

import java.util.Set;

/**
 * 由宿主从认证和数据库构造的可信查询主体。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record DataPermissionSubject(
        String tenant,
        String userId,
        Option<Long> departmentId,
        Set<String> roles,
        boolean administrator
) {
    public DataPermissionSubject {
        roles = Set.copyOf(roles);
    }
}
