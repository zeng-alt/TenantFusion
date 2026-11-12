package com.github.zeng.alt.admin.query.service;

import com.github.zeng.alt.admin.infrastructure.entity.User;
import com.github.zeng.alt.security.api.LoginHelper;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 验证停用用户会撤销全部会话，普通资料更新保留会话。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class UserSecurityServiceTest {
    private final RbacResourceService rbac = mock(RbacResourceService.class);
    private final LoginHelper login = mock(LoginHelper.class);
    private final UserSecurityService service = new UserSecurityService(rbac, login);

    @Test
    void shouldInvalidateRolesAndRevokeDisabledUserSessions() {
        final var user = new User();
        user.setUserId(7L);
        user.setEnabled(false);

        service.update(user);

        verify(rbac).removeUserRole(List.of("7"));
        verify(login).logout("7");
    }

    @Test
    void shouldKeepActiveUserSessionsWhenInvalidatingRoles() {
        final var user = new User();
        user.setUserId(7L);

        service.update(user);

        verify(rbac).removeUserRole(List.of("7"));
        verifyNoInteractions(login);
    }
}
