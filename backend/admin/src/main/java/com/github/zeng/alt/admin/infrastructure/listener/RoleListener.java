package com.github.zeng.alt.admin.infrastructure.listener;

import com.github.zeng.alt.admin.infrastructure.entity.Role;
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
public class RoleListener {

    @Resource
    private AuthHelper authHelper;

    public RoleListener() {
        ApplicationContextHelper.autowireBean(this);
    }

    @PreRemove
    public void preRemove(Role role) {
        if (authHelper.isSuperAdmin(role.getCode())) {
            throw new BaseException("超级管理员角色不能修改删除");
        }
    }
}
