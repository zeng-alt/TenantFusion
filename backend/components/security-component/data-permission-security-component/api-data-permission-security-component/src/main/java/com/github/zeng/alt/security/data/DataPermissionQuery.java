package com.github.zeng.alt.security.data;

import java.util.Set;

/**
 * 已解析的数据范围，租户条件始终独立于授权范围。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record DataPermissionQuery(
        String tenant,
        String userId,
        boolean all,
        boolean self,
        Set<Long> departmentIds
) {
    public DataPermissionQuery {
        departmentIds = Set.copyOf(departmentIds);
    }
}
