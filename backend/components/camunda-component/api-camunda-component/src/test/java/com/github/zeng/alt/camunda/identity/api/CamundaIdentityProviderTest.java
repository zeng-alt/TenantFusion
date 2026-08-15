package com.github.zeng.alt.camunda.identity.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CamundaIdentityProviderTest {

    private final CamundaUserGroupSource stubSource = new CamundaUserGroupSource() {
        @Override
        public Optional<CamundaIdentityUser> findByUsername(String username) {
            return "admin".equals(username)
                    ? Optional.of(new CamundaIdentityUser("admin", "Admin", "User", "admin@test.com"))
                    : Optional.empty();
        }

        @Override
        public boolean matchesPassword(String username, String rawPassword) {
            return "admin".equals(username) && "123456".equals(rawPassword);
        }

        @Override
        public List<CamundaIdentityGroup> findGroupsByUsername(String username) {
            return "admin".equals(username)
                    ? List.of(new CamundaIdentityGroup("ADMIN", "超级管理员", null))
                    : List.of();
        }

        @Override
        public List<CamundaIdentityUser> findUsersByGroupCode(String code) {
            return List.of();
        }

        @Override
        public Optional<CamundaIdentityGroup> findByGroupCode(String code) {
            return Optional.empty();
        }

        @Override
        public List<CamundaIdentityGroup> findAllGroups() {
            return List.of();
        }
    };

    private final CamundaIdentityProvider provider = new CamundaIdentityProvider(stubSource);

    @Test
    void findUserById_existingUser_returnsEntity() {
        var user = provider.findUserById("admin");
        assertNotNull(user);
        assertEquals("admin", user.getId());
        assertEquals("Admin", user.getFirstName());
        assertEquals("User", user.getLastName());
    }

    @Test
    void findUserById_unknownUser_returnsNull() {
        assertNull(provider.findUserById("nobody"));
    }

    @Test
    void checkPassword_correct_returnsTrue() {
        assertTrue(provider.checkPassword("admin", "123456"));
    }

    @Test
    void checkPassword_wrong_returnsFalse() {
        assertFalse(provider.checkPassword("admin", "wrong"));
    }

    @Test
    void groupQuery_byMember_returnsGroups() {
        // 直接构造无 CommandExecutor 的查询，避免单元测试依赖 ProcessEngine 上下文
        var groups = new CamundaGroupQuery(provider)
                .groupMember("admin")
                .list();
        assertEquals(1, groups.size());
        assertEquals("ADMIN", groups.get(0).getId());
    }
}
