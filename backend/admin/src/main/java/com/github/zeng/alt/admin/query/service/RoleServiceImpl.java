package com.github.zeng.alt.admin.query.service;

import com.github.zeng.alt.admin.infrastructure.entity.Role;
import com.github.zeng.alt.admin.infrastructure.entity.User;
import com.github.zeng.alt.admin.infrastructure.entity.UserRole;
import com.github.zeng.alt.admin.infrastructure.repository.RoleRepository;
import com.github.zeng.alt.admin.infrastructure.repository.UserRepository;
import com.github.zeng.alt.admin.infrastructure.repository.UserRoleRepository;
import com.github.zeng.alt.admin.query.api.RoleService;
import com.github.zeng.alt.api.exception.BaseException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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

    @Override
    @Transactional(rollbackOn = Exception.class)
    public void addRoleUsers(Long roleId, List<Long> userIds) {
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
        if (roleRepository.findById(roleId).isEmpty()) {
            throw new BaseException("角色不存在");
        }
        for (Long userId : userIds) {
            userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);
        }
    }
}
