package com.github.zeng.alt.admin.query.api;

import com.github.zeng.alt.admin.query.api.dto.AuthorizePermissionDto;
import com.github.zeng.alt.admin.query.api.dto.CreateRoleDto;
import com.github.zeng.alt.admin.query.api.dto.PatchRoleDto;
import io.vavr.control.Either;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Set;

/**
 * @author zengJiaJun
 * @since 2026年07月20日
 * @version 1.0
 */
public interface RoleService {

    void create(CreateRoleDto dto);

    Either<String, Long> patchRole(Long id, PatchRoleDto dto);

    void addRoleUsers(Long roleId, List<Long> userIds);

    void removeRoleUsers(Long roleId, List<Long> userIds);

    void authorizePermission(AuthorizePermissionDto dto);

    Set<String> getRoleCodes(boolean enabled);

    default Set<String> getRoleCodes() {
        return getRoleCodes(true);
    }
}

