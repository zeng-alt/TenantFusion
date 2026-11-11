package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.infrastructure.entity.User;
import com.github.zeng.alt.admin.infrastructure.repository.UserRepository;
import com.github.zeng.alt.security.abac.AbacMethodContext;

import io.vavr.control.Option;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 用户资源解析器：从 #id 查库映射标量属性，非法 ID 与缺失资源 fail-closed。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class UserResourceResolverTest {
    private final UserRepository users = mock(UserRepository.class);
    private final UserResourceResolver resolver =
            new UserResourceResolver(users, Mappers.getMapper(AbacUserMapper.class));

    private AbacMethodContext context(Object id) throws Exception {
        return new AbacMethodContext(Object.class.getMethod("toString"), UserResourceResolver.class,
                new Object[]{}, Map.of("id", id), "user:update");
    }

    @Test
    void shouldResolveTrustedAttributesFromDatabase() throws Exception {
        final User user = new User();
        user.setUserId(42L);
        user.setDeptId(20L);
        user.setTenantBy("t001");
        when(users.findById(42L)).thenReturn(Option.of(user));

        final Map<String, Object> attributes = resolver.resolve(context(42L));

        assertThat(attributes.get("id")).isEqualTo("42");
        assertThat(attributes.get("deptId")).isEqualTo(20L);
        assertThat(attributes.get("tenant")).isEqualTo("t001");
    }

    @Test
    void shouldRejectMalformedResourceId() throws Exception {
        assertThatThrownBy(() -> resolver.resolve(context("abc"))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldRejectMissingResource() throws Exception {
        when(users.findById(42L)).thenReturn(Option.none());

        assertThatThrownBy(() -> resolver.resolve(context(42L))).isInstanceOf(IllegalStateException.class);
    }
}