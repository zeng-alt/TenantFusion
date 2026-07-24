package com.github.zeng.alt.admin.query.service;

import com.github.zeng.alt.admin.infrastructure.entity.Role;
import com.github.zeng.alt.admin.infrastructure.entity.User;
import com.github.zeng.alt.admin.infrastructure.entity.UserRole;
import com.github.zeng.alt.admin.infrastructure.repository.RoleRepository;
import com.github.zeng.alt.admin.infrastructure.repository.UserRepository;
import com.github.zeng.alt.admin.infrastructure.repository.UserRoleRepository;
import com.github.zeng.alt.admin.query.api.UserService;
import com.github.zeng.alt.admin.query.api.dto.*;
import com.github.zeng.alt.admin.query.service.transformation.UserDtoTransformation;
import com.github.zeng.alt.api.exception.BaseException;
import com.github.zeng.alt.bean.BeanHelper;
import com.github.zeng.alt.security.api.AuthHelper;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zengJiaJun
 * @since 2026年07月14日
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final SecurityProperties securityProperties;
    private final PasswordEncoder passwordEncoder;
    private final UserDtoTransformation userDtoTransformation;
    private final AuthHelper authHelper;

    @Override
    @Transactional(readOnly = true)
    public CurrentUserDto currentUser() {
        SecurityUser securityUser = UserContextHolder.getSecurityUser();

        if (securityUser == null || !StringUtils.hasText(securityUser.getId())) {
            throw new BaseException(11008, "当前用户未登录");
        }

        long userId;
        try {
            userId = Long.parseLong(securityUser.getId());
        } catch (NumberFormatException e) {
            throw new BaseException("用户id格式错误");
        }

        User user = userRepository.findById(userId)
                .getOrElseThrow(() -> new BaseException(11008, "用户不存在"));

        if (BooleanUtils.isFalse(user.getEnabled())) {
            throw new BaseException(11008, "用户已被禁用");
        }

        return convertToCurrentUserDto(user, securityUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(CreateUserDto dto) {
        dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        User user = userRepository.save(userDtoTransformation.toEntity(dto));
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {

            List<Role> roles = roleRepository.findByIdIn(dto.getRoleIds());

            List<UserRole> userRoles = roles.stream()
                    .map(role -> {
                        UserRole ur = new UserRole();
                        ur.setUser(user);
                        ur.setRole(role);
                        return ur;
                    })
                    .toList();

            userRoleRepository.saveAll(userRoles);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Either<String, Long> patchUser(Long id, PatchUserDto dto) {
        if (authHelper.isSuperAdmin(id)) {
            throw new BaseException("内置超级用户不能修改");
        }

        return userRepository.findById(id)
                .toEither("用户不存在")
                .map(user -> {
                    userDtoTransformation.mergeEntity(dto, user);
                    Optional.ofNullable(dto.getRoleIds())
                            .ifPresent(
                                    roleIds -> updateUserRoles(user.getId(), roleIds)
                            );
                    return user.getId();
                });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Either<String, Long> resetPassword(Long id, ResetUserPasswordDto dto) {
        if (authHelper.isSuperAdmin(id)) {
            throw new BaseException("内置超级用户不能修改密码");
        }
        return userRepository.findById(id)
                .toEither("用户不存在")
                .map(user -> {
                    dto.setPassword(passwordEncoder.encode(dto.getPassword()));
                    userDtoTransformation.mergeEntity(dto, user);
                    userRepository.save(user);
                    return user.getId();
                });
    }

    private void updateUserRoles(Long userId, List<Long> roleIds) {

        Set<Long> newRoleIds = new HashSet<>(roleIds);
        List<UserRole> oldRelations =
                userRoleRepository.findByUserId(userId);

        Set<Long> oldRoleIds = oldRelations.stream()
                .map(x -> x.getRole().getId())
                .collect(Collectors.toSet());

        // 删除
        List<UserRole> deletes = oldRelations.stream()
                .filter(x -> !newRoleIds.contains(x.getRole().getId()))
                .toList();

        if (!CollectionUtils.isEmpty(deletes)) {
            userRoleRepository.deleteAll(deletes);
        }

        // 新增
        Set<Long> addRoleIds = new HashSet<>(newRoleIds);
        addRoleIds.removeAll(oldRoleIds);

        if (!addRoleIds.isEmpty()) {
            List<Role> roles =
                    roleRepository.findByIdIn(addRoleIds);

            List<UserRole> additions =
                    roles.stream()
                            .map(role -> {
                                UserRole ur = new UserRole();
                                User user = new User();
                                user.setUserId(userId);
                                ur.setUser(user);
                                ur.setRole(role);
                                return ur;
                            })
                            .toList();

            userRoleRepository.saveAll(additions);
        }
    }

    private CurrentUserDto convertToCurrentUserDto(
            User user,
            SecurityUser securityUser
    ) {

        CurrentUserDto dto = BeanHelper.copyToObject(user, CurrentUserDto.class);

        ProfileDto profile = BeanHelper.copyToObject(
                user,
                ProfileDto.class
        );

        dto.setProfile(profile);

        List<UserRole> userRoles = Optional.ofNullable(user.getUserRoles())
                .orElse(Collections.emptyList());


        List<RoleDto> roles = userRoles.stream()
                .filter(Objects::nonNull)
                .map(UserRole::getRole)
                .filter(Objects::nonNull)
                .map(role -> BeanHelper.copyToObject(role, RoleDto.class))
                .toList();

        dto.setRoles(roles);


        RoleDto currentRole = getCurrentRole(
                userRoles,
                securityUser
        );

        dto.setCurrentRole(currentRole);

        if (securityProperties.getAdmin().getId().equals(securityUser.getId())) {
            dto.setCurrentRole(BeanHelper.copyToObject(securityProperties.getAdmin(), RoleDto.class));
        }

        return dto;
    }

    private RoleDto getCurrentRole(
            List<UserRole> userRoles,
            SecurityUser securityUser
    ) {

        if (securityUser.getCurrentRole() == null) {
            return null;
        }

        String roleCode = securityUser
                .getCurrentRole()
                .getAuthority();

        if (!StringUtils.hasText(roleCode)) {
            return null;
        }


        return userRoles.stream()
                .map(UserRole::getRole)
                .filter(Objects::nonNull)
                .filter(role -> roleCode.equals(role.getCode()))
                .findFirst()
                .map(role -> BeanHelper.copyToObject(role, RoleDto.class))
                .orElse(null);
    }
}
