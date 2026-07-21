package com.github.zeng.alt.admin.query.api;

import java.util.List;

/**
 * @author zengJiaJun
 * @since 2026年07月20日
 * @version 1.0
 */
public interface RoleService {

    void addRoleUsers(Long roleId, List<Long> userIds);

    void removeRoleUsers(Long roleId, List<Long> userIds);
}

