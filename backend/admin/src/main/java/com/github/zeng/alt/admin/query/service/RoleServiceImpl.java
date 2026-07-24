package com.github.zeng.alt.admin.query.service;

import com.github.zeng.alt.admin.infrastructure.entity.Permission;
import com.github.zeng.alt.admin.infrastructure.entity.Role;
import com.github.zeng.alt.admin.infrastructure.entity.RolePermission;
import com.github.zeng.alt.admin.infrastructure.entity.User;
import com.github.zeng.alt.admin.infrastructure.entity.UserRole;
import com.github.zeng.alt.admin.infrastructure.repository.PermissionRepository;
import com.github.zeng.alt.admin.infrastructure.repository.RolePermissionRepository;
import com.github.zeng.alt.admin.infrastructure.repository.RoleRepository;
import com.github.zeng.alt.admin.infrastructure.repository.UserRepository;
import com.github.zeng.alt.admin.infrastructure.repository.UserRoleRepository;
import com.github.zeng.alt.admin.query.api.RoleService;
import com.github.zeng.alt.admin.query.api.dto.AuthorizePermissionDto;
import com.github.zeng.alt.admin.query.api.dto.CreateRoleDto;
import com.github.zeng.alt.admin.query.api.dto.PatchRoleDto;
import com.github.zeng.alt.admin.query.service.transformation.RoleDtoTransformation;
import com.github.zeng.alt.api.exception.BaseException;
import com.github.zeng.alt.security.api.AuthHelper;
import io.vavr.control.Either;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zengJiaJun
 * @since 2026年07月20日
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final RoleDtoTransformation roleDtoTransformation;
    private final AuthHelper authHelper;

    @Override
    @Transactional(rollbackOn = Exception.class)
    public void create(CreateRoleDto dto) {
        Role role = roleRepository.save(roleDtoTransformation.toEntity(dto));
        saveRolePermissions(role, dto.getPermissionIds());
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public Either<String, Long> patchRole(Long id, PatchRoleDto dto) {
        return roleRepository.findById(id)
                .toEither("角色不存在")
                .map(role -> {
                    if (authHelper.isAdmin(role.getCode())) {
                        throw new BaseException("超级管理员角色不能修改");
                    }
                    roleDtoTransformation.mergeEntity(dto, role);
                    Optional.ofNullable(dto.getPermissionIds())
                            .ifPresent(
                                    roleIds -> updateRolePermissions(role.getId(), roleIds)
                            );
                    return role.getId();
                });
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public void addRoleUsers(Long roleId, List<Long> userIds) {
        if (userIds.stream().anyMatch(authHelper::isSuperAdmin)) {
            throw new BaseException("内置超级用户不能修改");
        }
        Role role = roleRepository.findById(roleId).getOrNull();
        if (role == null) {
            throw new BaseException("角色不存在");
        }
        for (Long userId : userIds) {
            if (!userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
                User user = userRepository.findById(userId).getOrNull();
                if (user != null) {
                    UserRole userRole = new UserRole();
                    userRole.setUser(user);
                    userRole.setRole(role);
                    userRoleRepository.save(userRole);
                }
            }
        }

    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public void removeRoleUsers(Long roleId, List<Long> userIds) {
        if (userIds.stream().anyMatch(authHelper::isSuperAdmin)) {
            throw new BaseException("内置超级用户不能修改");
        }
        if (roleRepository.findById(roleId).isEmpty()) {
            throw new BaseException("角色不存在");
        }
        for (Long userId : userIds) {
            userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);
        }
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public void authorizePermission(AuthorizePermissionDto dto) {
        List<Role> roles = roleRepository.findByIdIn(dto.getRoleIds());
        if (roles.stream().anyMatch(r -> authHelper.isAdmin(r.getCode()))) {
            throw new BaseException("超级管理员角色不能修改");
        }
        if (roles.size() != dto.getRoleIds().size()) {
            throw new BaseException("存在不存在的角色");
        }

        List<Long> permissionIds = Optional.ofNullable(dto.getPermissionIds())
                .orElse(Collections.emptyList());

        // 权限为空，清空角色所有权限
        if (permissionIds.isEmpty()) {
            return;
        }

        // 校验权限
        List<Permission> permissions =
                permissionRepository.findByIdIn(permissionIds);
        if (permissions.size() != permissionIds.size()) {
            throw new BaseException("存在不存在的权限");
        }

        // 同步权限
        roles.forEach(role ->
                updateRolePermissions(
                        role.getId(),
                        permissionIds,
                        false
                )
        );

    }

    private void saveRolePermissions(Role role, List<Long> permissionIds) {
        if (CollectionUtils.isEmpty(permissionIds)) return;
        permissionRepository.findAllByPermissionIdIn(permissionIds)
                .forEach(permission -> {
                    RolePermission rp = new RolePermission();
                    rp.setRole(role);
                    rp.setPermission(permission);
                    rolePermissionRepository.save(rp);
                });
    }

    private void updateRolePermissions(Long roleId, List<Long> permissionIds) {
        updateRolePermissions(roleId, permissionIds, true);
    }

    private void updateRolePermissions(Long roleId, List<Long> permissionIds, boolean isDelete) {

        Set<Long> newPermissionIds = new HashSet<>(permissionIds);
        List<RolePermission> oldRelations =
                rolePermissionRepository.findByRoleId(roleId);

        Set<Long> oldPermissionIds = oldRelations.stream()
                .map(x -> x.getPermission().getId())
                .collect(Collectors.toSet());

        if (isDelete) {
            // 删除
            List<RolePermission> deletes = oldRelations.stream()
                    .filter(x -> !newPermissionIds.contains(x.getPermission().getId()))
                    .toList();

            if (!CollectionUtils.isEmpty(deletes)) {
                rolePermissionRepository.deleteAll(deletes);
            }
        }

        // 新增
        Set<Long> addPermissionIds = new HashSet<>(newPermissionIds);
        addPermissionIds.removeAll(oldPermissionIds);

        if (!addPermissionIds.isEmpty()) {
            List<Permission> permissions =
                    permissionRepository.findByIdIn(addPermissionIds);

            List<RolePermission> additions =
                    permissions.stream()
                            .map(permission -> {
                                RolePermission rp = new RolePermission();
                                Role role = new Role();
                                role.setRoleId(roleId);
                                rp.setRole(role);
                                rp.setPermission(permission);
                                return rp;
                            })
                            .toList();

            rolePermissionRepository.saveAll(additions);
        }
    }
}
