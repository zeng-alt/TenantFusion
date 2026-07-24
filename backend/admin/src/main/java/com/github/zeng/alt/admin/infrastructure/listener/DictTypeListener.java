package com.github.zeng.alt.admin.infrastructure.listener;

import com.github.zeng.alt.admin.infrastructure.entity.DictType;
import com.github.zeng.alt.api.exception.ForbiddenException;
import com.github.zeng.alt.bean.ApplicationContextHelper;
import com.github.zeng.alt.security.api.AuthHelper;
import jakarta.annotation.Resource;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import org.apache.commons.lang3.BooleanUtils;

/**
 * @author zengJiaJun
 * @since 2026年07月23日
 * @version 1.0
 */
public class DictTypeListener {

    @Resource
    private AuthHelper authHelper;

    public DictTypeListener() {
        ApplicationContextHelper.autowireBean(this);
    }

    @PostLoad
    public void postLoad(DictType entity) {
        entity.setOldIsDefault(entity.getIsDefault());
    }

    @PrePersist
    public void prePersist(DictType entity) {
        if (!authHelper.isAdmin() && Boolean.TRUE.equals(entity.getIsDefault())) {
            throw new ForbiddenException("当前用户权限无法修改字典类型的【是否默认】属性");
        }
    }

    @PreUpdate
    public void preUpdate(DictType entity) {
        if (!authHelper.isAdmin() && BooleanUtils.compare(entity.getOldIsDefault(), entity.getIsDefault()) != 0) {
            throw new ForbiddenException("当前用户权限无法修改字典类型的【是否默认】属性");
        }
    }

    @PreRemove
    public void preRemove(DictType entity) {
        if (!authHelper.isAdmin() && BooleanUtils.isTrue(entity.getIsDefault())) {
            throw new ForbiddenException("当前用户权限无法删除系统默认的字典类型");
        }
    }
}
