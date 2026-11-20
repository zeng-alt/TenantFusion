package com.github.zeng.alt.workflow.service.impl;

import com.github.zeng.alt.api.exception.BaseException;
import com.github.zeng.alt.camunda.engine.api.task.TaskApi;
import com.github.zeng.alt.camunda.engine.api.task.TaskInfo;
import com.github.zeng.alt.security.api.AuthorityResolver;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.security.core.AuthHelperImpl;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 任务取消认领保持认领人、管理员及以上操作权限。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class TaskServiceImplTest {

    private final TaskApi tasks = mock(TaskApi.class);
    private final SecurityProperties properties = new SecurityProperties();
    private final TaskServiceImpl service = new TaskServiceImpl(tasks, mock(), mock(), new AuthHelperImpl(
            properties, new StaticListableBeanFactory().getBeanProvider(AuthorityResolver.class)));

    TaskServiceImplTest() {
        properties.getAdmin().setCode("admin");
    }

    @AfterEach
    void clearContext() {
        UserContextHolder.clear();
    }

    @ParameterizedTest
    @CsvSource({"1, , other, true", "7, ADMIN, other, true", "7, USER, review, true", "7, USER, other, false"})
    void unclaimRespectsAssigneeAndAdministratorAccess(String id, String role, String assignee, boolean granted) {
        login(id, role);
        when(tasks.get("task-1")).thenReturn(TaskInfo.builder().id("task-1").assignee(assignee).build());

        if (granted) {
            assertThatCode(() -> service.unclaimTask("task-1")).doesNotThrowAnyException();
            verify(tasks).unclaim("task-1");
            return;
        }
        assertThatThrownBy(() -> service.unclaimTask("task-1")).isInstanceOf(BaseException.class);
        verify(tasks, never()).unclaim("task-1");
    }

    @Test
    void disabledAdministratorExemptionDoesNotAllowUnclaimingAnotherUsersTask() {
        properties.getAdmin().setEnabled(false);
        login("1", null);
        when(tasks.get("task-1")).thenReturn(TaskInfo.builder().id("task-1").assignee("other").build());

        assertThatThrownBy(() -> service.unclaimTask("task-1")).isInstanceOf(BaseException.class);
        verify(tasks, never()).unclaim("task-1");
    }

    private void login(String id, String role) {
        final SimpleGrantedAuthority currentRole = role == null ? null : new SimpleGrantedAuthority(role);
        final SecurityUser user = SecurityUser.withUsername("review").id(id).tenant("master")
                .roles(currentRole == null ? Set.of() : Set.of(currentRole)).currentRole(currentRole).build();
        UserContextHolder.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                user, null, user.getAuthorities()));
    }
}
