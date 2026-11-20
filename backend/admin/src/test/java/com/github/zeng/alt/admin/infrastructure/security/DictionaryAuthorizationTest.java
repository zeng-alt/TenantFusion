package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.infrastructure.entity.DictData;
import com.github.zeng.alt.admin.infrastructure.entity.DictType;
import com.github.zeng.alt.admin.infrastructure.entity.Role;
import com.github.zeng.alt.admin.infrastructure.entity.User;
import com.github.zeng.alt.admin.infrastructure.listener.DictDataListener;
import com.github.zeng.alt.admin.infrastructure.listener.DictTypeListener;
import com.github.zeng.alt.admin.infrastructure.listener.RoleListener;
import com.github.zeng.alt.admin.infrastructure.listener.UserListener;
import com.github.zeng.alt.api.exception.BaseException;
import com.github.zeng.alt.api.exception.ForbiddenException;
import com.github.zeng.alt.bean.ApplicationContextHelper;
import com.github.zeng.alt.security.api.AuthHelper;
import com.github.zeng.alt.security.api.AuthorityResolver;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.security.core.AuthHelperImpl;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 使用真实身份上下文、AuthHelper 和 JPA 监听器验证字典操作与内置对象保护。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class DictionaryAuthorizationTest {

    private final ApplicationContext previousContext = ApplicationContextHelper.getApplicationContext();
    private final ConfigurableListableBeanFactory previousBeanFactory = (ConfigurableListableBeanFactory)
            ReflectionTestUtils.getField(ApplicationContextHelper.class, "beanFactory");
    private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    private final SecurityProperties properties = new SecurityProperties();
    private final DictType type = new DictType();
    private final DictData entry = new DictData();
    private DictTypeListener types;
    private DictDataListener entries;

    @BeforeEach
    void configureListeners() {
        properties.getAdmin().setCode("admin");
        context.registerBean(AuthHelper.class, () -> new AuthHelperImpl(
                properties, context.getBeanProvider(AuthorityResolver.class)));
        context.registerBean(ApplicationContextHelper.class);
        context.refresh();
        types = new DictTypeListener();
        entries = new DictDataListener();
        types.postLoad(type);
        entries.postLoad(entry);
        type.setIsDefault(true);
        entry.setIsDefault(true);
    }

    @AfterEach
    void clearContexts() {
        UserContextHolder.clear();
        context.close();
        ReflectionTestUtils.setField(ApplicationContextHelper.class, "applicationContext", previousContext);
        ReflectionTestUtils.setField(ApplicationContextHelper.class, "beanFactory", previousBeanFactory);
    }

    @ParameterizedTest
    @CsvSource({"1, ", "1, USER", "7, ADMIN"})
    void administratorAndHigherCanManageDefaultDictionaryEntries(String id, String role) {
        login(id, role);

        assertDefaultMutationsAllowed();
    }

    @Test
    void ordinaryUserCannotManageDefaultDictionaryEntries() {
        login("7", "USER");

        assertDefaultsProtected();
    }

    @Test
    void disabledAdministratorExemptionDoesNotGrantDictionaryAccess() {
        properties.getAdmin().setEnabled(false);
        login("1", null);

        assertDefaultsProtected();
    }

    @Test
    void ordinaryUserCanManageNonDefaultDictionaryEntries() {
        login("7", "USER");
        type.setIsDefault(false);
        entry.setIsDefault(false);

        assertDefaultMutationsAllowed();
    }

    @Test
    void ordinaryUserCanEditOtherFieldsWithoutChangingDefaultFlag() {
        login("7", "USER");
        types.postLoad(type);
        entries.postLoad(entry);

        assertThatCode(() -> types.preUpdate(type)).doesNotThrowAnyException();
        assertThatCode(() -> entries.preUpdate(entry)).doesNotThrowAnyException();
    }

    @Test
    void builtInRoleAndUserRemainProtectedWhenExemptionIsDisabled() {
        properties.getAdmin().setEnabled(false);
        final Role role = new Role();
        role.setCode("ADMIN");
        final User user = new User();
        user.setUserId(1L);

        assertThatThrownBy(() -> new RoleListener().preRemove(role))
                .isInstanceOf(BaseException.class).hasMessage("超级管理员角色不能修改删除");
        assertThatThrownBy(() -> new UserListener().preRemove(user))
                .isInstanceOf(BaseException.class).hasMessage("内置超级用户不能删除");
        role.setCode("1");
        assertThatCode(() -> new RoleListener().preRemove(role)).doesNotThrowAnyException();
    }

    private void assertDefaultMutationsAllowed() {
        assertThatCode(() -> types.prePersist(type)).doesNotThrowAnyException();
        assertThatCode(() -> types.preUpdate(type)).doesNotThrowAnyException();
        assertThatCode(() -> types.preRemove(type)).doesNotThrowAnyException();
        assertThatCode(() -> entries.prePersist(entry)).doesNotThrowAnyException();
        assertThatCode(() -> entries.preUpdate(entry)).doesNotThrowAnyException();
        assertThatCode(() -> entries.preRemove(entry)).doesNotThrowAnyException();
    }

    private void assertDefaultsProtected() {
        assertThatThrownBy(() -> types.prePersist(type)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> types.preUpdate(type)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> types.preRemove(type)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> entries.prePersist(entry)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> entries.preUpdate(entry)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> entries.preRemove(entry)).isInstanceOf(ForbiddenException.class);
    }

    private void login(String id, String role) {
        final SimpleGrantedAuthority currentRole = role == null ? null : new SimpleGrantedAuthority(role);
        final SecurityUser user = SecurityUser.withUsername("review").id(id).tenant("master")
                .roles(currentRole == null ? Set.of() : Set.of(currentRole)).currentRole(currentRole).build();
        UserContextHolder.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                user, null, user.getAuthorities()));
    }
}
