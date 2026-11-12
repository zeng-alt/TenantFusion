package com.github.zeng.alt.admin.query.service;

import com.github.zeng.alt.admin.infrastructure.entity.User;
import com.github.zeng.alt.security.api.LoginHelper;
import com.github.zeng.alt.security.rbac.serve.repository.RbacResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户更新后的授权缓存失效与停用会话撤销。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Service
@RequiredArgsConstructor
public class UserSecurityService {
    private final RbacResourceService rbacResourceService;
    private final LoginHelper loginHelper;

    /** 更新用户的授权缓存，并撤销停用用户的全部登录凭证。 */
    @Transactional(rollbackFor = Exception.class)
    public void update(User user) {
        final String userId = String.valueOf(user.getId());
        rbacResourceService.removeUserRole(List.of(userId));
        if (Boolean.FALSE.equals(user.getEnabled())) {
            loginHelper.logout(userId);
        }
    }
}
