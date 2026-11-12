package com.github.zeng.alt.security.core;

import com.github.zeng.alt.security.api.LoginHelper;
import com.github.zeng.alt.security.api.LoginResponse;
import com.github.zeng.alt.security.api.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证登录助手不再静默接受未实现的凭证操作。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class LoginHelperTest {
    private final SecurityUser user = SecurityUser.withUsername("review").id("7").password("unused").build();

    @Test
    void shouldRejectUnsupportedResetAndUserLogout() {
        final LoginHelper helper = new UnsupportedHelper();
        assertThatThrownBy(() -> helper.reset(user, false)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> helper.reset(user, false,
                new MockHttpServletRequest(), new MockHttpServletResponse()))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> helper.logout("7")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> helper.logout(7L)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldDelegateServletResetToSupportedReset() {
        final LoginHelper helper = new UnsupportedHelper() {
            @Override
            public LoginResponse reset(SecurityUser principal, boolean rememberMe) {
                return LoginResponse.ofUser(principal);
            }
        };
        assertThat(helper.reset(user, false, new MockHttpServletRequest(), new MockHttpServletResponse()))
                .isNotNull();
    }

    private static class UnsupportedHelper implements LoginHelper {
        public String name() { return "unsupported"; }
        public LoginResponse login(String username, String password) { throw new UnsupportedOperationException(); }
        public void logout(HttpServletRequest request) { }
        public SecurityUser getCurrentUser() { throw new UnsupportedOperationException(); }
    }
}
