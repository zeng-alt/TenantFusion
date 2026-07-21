package com.github.zeng.alt.admin.query.service;

import com.github.zeng.alt.admin.infrastructure.entity.User;
import com.github.zeng.alt.admin.infrastructure.entity.UserRole;
import com.github.zeng.alt.admin.infrastructure.repository.UserRepository;
import com.github.zeng.alt.admin.query.api.UserService;
import com.github.zeng.alt.admin.query.api.dto.CurrentUserDto;
import com.github.zeng.alt.admin.query.api.dto.ProfileDto;
import com.github.zeng.alt.admin.query.api.dto.RoleDto;
import com.github.zeng.alt.api.exception.BaseException;
import com.github.zeng.alt.bean.BeanHelper;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @author zengJiaJun
 * @since 2026年07月14日
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final SecurityProperties securityProperties;

    @Override
    @Transactional(readOnly = true)
    public CurrentUserDto currentUser() {

        SecurityUser securityUser = UserContextHolder.getSecurityUser();

        if (securityUser == null || !StringUtils.hasText(securityUser.getId())) {
            throw new BaseException("当前用户未登录，请重新登录");
        }

        long userId;
        try {
            userId = Long.parseLong(securityUser.getId());
        } catch (NumberFormatException e) {
            throw new BaseException("用户id格式错误");
        }

        User user = userRepository.findById(userId)
                .getOrElseThrow(() -> new BaseException("用户不存在"));

        return convertToCurrentUserDto(user, securityUser);
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
