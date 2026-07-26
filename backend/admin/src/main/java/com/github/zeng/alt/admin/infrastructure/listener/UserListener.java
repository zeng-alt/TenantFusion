package com.github.zeng.alt.admin.infrastructure.listener;

import com.github.zeng.alt.admin.infrastructure.entity.User;
import com.github.zeng.alt.api.exception.BaseException;
import com.github.zeng.alt.bean.ApplicationContextHelper;
import com.github.zeng.alt.security.api.AuthHelper;
import jakarta.annotation.Resource;
import jakarta.persistence.PreRemove;

/**
 * @author zengJiaJun
 * @since 2026年07月22日
 * @version 1.0
 */
public class UserListener {

    @Resource
    private AuthHelper authHelper;

    public UserListener() {
        ApplicationContextHelper.autowireBean(this);
    }

    @PreRemove
    public void preRemove(User user) {
        if (authHelper.isSuperAdmin(user.getId())) {
            throw new BaseException("内置超级用户不能删除");
        }
    }
}
