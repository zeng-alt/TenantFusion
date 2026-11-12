package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceService;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.stream.IntStream;

/**
 * 用户数据权限集成测试的租户、组织、角色和规则夹具。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
final class UserDataPermissionTestHelper {
    private final JdbcTemplate jdbc;
    private final RbacResourceService rbac;
    private static final List<String> ROLES = List.of(
            "DP_SELF", "DP_DEPT", "DP_TREE", "DP_SET", "DP_ALL", "DP_EMPTY", "DP_DENIED");
    private static final List<String> PERMISSIONS = List.of("GET:/v1/user", "GET:/v1/user/{id:\\d+}",
            "GET:/v1/user/info", "POST:/v1/user/search");

    UserDataPermissionTestHelper(JdbcTemplate jdbc, RbacResourceService rbac) {
        this.jdbc = jdbc;
        this.rbac = rbac;
    }

    void seed() {
        jdbc.update("delete from main_policy_rule where permission_id in (10049, 10050, 20261112001, 20261112002)");
        jdbc.update("delete from main_data_permission where role_code like 'DP_%'");
        seedDepartmentsAndUsers();
        seedRolesAndPermissions();
        jdbc.update("""
                insert into main_user_role (user_role_id, user_id, role_id, tenant_by, is_deleted)
                values (858888, 810002, 840001, 'master', false) on conflict (user_role_id) do nothing
                """);
        jdbc.update("""
                insert into main_role_permission (role_permission_id, role_id, permission_id, tenant_by, is_deleted)
                select 880001, 840006, permission_id, 'master', false from main_permission
                where method='GET' and path='/v1/role' and tenant_by='master'
                on conflict (role_permission_id) do nothing
                """);
        rbac.removeUserRole(List.of("810001"), "master");
        rbac.removeRolePermission(ROLES, "master");
    }

    private void seedDepartmentsAndUsers() {
        jdbc.update("""
                insert into main_department (dept_id, dept_name, parent_id, tenant_by, is_enabled, is_deleted)
                values (830010, 'dp-root', null, 'master', true, false),
                       (830020, 'dp-child', 830010, 'master', true, false),
                       (830030, 'dp-selected', null, 'master', true, false),
                       (830040, 'dp-foreign', 830010, 't002', true, false)
                on conflict (dept_id) do nothing
                """);
        jdbc.update("""
                insert into main_user (user_id, username, password, dept_id, tenant_by, is_enabled, is_deleted)
                values (810001, 'dp-reader', '{noop}unused', 830010, 'master', true, false),
                       (810002, 'dp-peer', '{noop}unused', 830010, 'master', true, false),
                       (810003, 'dp-child', '{noop}unused', 830020, 'master', true, false),
                       (810004, 'dp-selected', '{noop}unused', 830030, 'master', true, false),
                       (810005, 'dp-foreign', '{noop}unused', 830040, 't002', true, false)
                on conflict (user_id) do nothing
                """);
    }

    private void seedRolesAndPermissions() {
        jdbc.update("""
                insert into main_permission
                  (permission_id, resource_type, code, name, method, path, tenant_by, is_enabled, is_deleted)
                values (20261112001, 'HTTP', 'GET:/v1/user/info', 'dp-info',
                        'GET', '/v1/user/info', 'master', true, false),
                       (20261112002, 'HTTP', 'POST:/v1/user/search', 'dp-search',
                        'POST', '/v1/user/search', 'master', true, false)
                on conflict (permission_id) do nothing
                """);
        IntStream.range(0, ROLES.size()).forEach(index -> {
            final long roleId = 840001L + index;
            jdbc.update("""
                    insert into main_role (role_id, code, name, tenant_by, is_enabled, is_deleted)
                    values (?, ?, ?, 'master', true, false) on conflict (role_id) do nothing
                    """, roleId, ROLES.get(index), ROLES.get(index));
            jdbc.update("""
                    insert into main_user_role (user_role_id, user_id, role_id, tenant_by, is_deleted)
                    values (?, 810001, ?, 'master', false) on conflict (user_role_id) do nothing
                    """, 850001L + index, roleId);
            seedRoleScopes(index, roleId);
        });
    }

    private void seedRoleScopes(int index, long roleId) {
        final List<Long> ids = List.of(10049L, 10050L, 20261112001L, 20261112002L);
        IntStream.range(0, ids.size()).forEach(operation -> {
            if (index == 6) {
                return;
            }
            jdbc.update("""
                    insert into main_role_permission (role_permission_id, role_id, permission_id, tenant_by, is_deleted)
                    values (?, ?, ?, 'master', false) on conflict (role_permission_id) do nothing
                    """, 860000L + index * 10 + operation, roleId, ids.get(operation));
            if (index >= 5) {
                return;
            }
            final String scope = List.of("SELF", "DEPT", "DEPT_TREE", "DEPT_SET", "ALL").get(index);
            final long policyId = 870000L + index * 10 + operation;
            jdbc.update("""
                    insert into main_data_permission
                      (data_permission_id, tenant_by, resource, permission, role_code, scope_type, is_enabled)
                    values (?, 'master', 'user', ?, ?, ?, true)
                    """, policyId, PERMISSIONS.get(operation), ROLES.get(index), scope);
            if (index == 3) {
                jdbc.update("insert into main_data_permission_department values (?, 830030)", policyId);
            }
        });
    }

}
