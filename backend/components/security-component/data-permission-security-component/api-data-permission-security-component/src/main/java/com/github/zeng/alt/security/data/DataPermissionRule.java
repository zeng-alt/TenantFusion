package com.github.zeng.alt.security.data;

import java.util.Set;

/**
 * 单条角色数据授权；指定部门集合为空时不授予任何部门。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record DataPermissionRule(DataPermissionScope scope, Set<Long> departmentIds) {
    public DataPermissionRule {
        departmentIds = Set.copyOf(departmentIds);
    }
}
