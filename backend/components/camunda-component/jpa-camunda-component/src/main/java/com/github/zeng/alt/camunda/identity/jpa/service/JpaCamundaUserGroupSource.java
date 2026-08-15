package com.github.zeng.alt.camunda.identity.jpa.service;

import com.github.zeng.alt.camunda.identity.api.CamundaIdentityGroup;
import com.github.zeng.alt.camunda.identity.api.CamundaIdentityUser;
import com.github.zeng.alt.camunda.identity.api.CamundaUserGroupSource;
import com.github.zeng.alt.camunda.identity.jpa.entity.MainUserEntity;
import com.github.zeng.alt.camunda.identity.jpa.repository.MainRoleRepository;
import com.github.zeng.alt.camunda.identity.jpa.repository.MainUserRepository;
import com.github.zeng.alt.camunda.identity.jpa.repository.MainUserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 基于 JPA 读取 admin 用户/角色表的 SPI 实现。
 */
@Component
@RequiredArgsConstructor
public class JpaCamundaUserGroupSource implements CamundaUserGroupSource {

    private final MainUserRepository userRepository;
    private final MainRoleRepository roleRepository;
    private final MainUserRoleRepository userRoleRepository;

    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Override
    public Optional<CamundaIdentityUser> findByUsername(String username) {
        return userRepository.findActiveByUsername(username)
                .map(this::toUser);
    }

    @Override
    public boolean matchesPassword(String username, String rawPassword) {
        Optional<MainUserEntity> userOpt = userRepository.findActiveByUsername(username);
        if (userOpt.isEmpty()) {
            return false;
        }
        MainUserEntity user = userOpt.get();
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    @Override
    public List<CamundaIdentityGroup> findGroupsByUsername(String username) {
        return userRoleRepository.findActiveByUserUsername(username).stream()
                .map(ur -> toGroup(ur.getRole()))
                .distinct()
                .toList();
    }

    @Override
    public List<CamundaIdentityUser> findUsersByGroupCode(String code) {
        return userRoleRepository.findActiveByRoleCode(code).stream()
                .map(ur -> toUser(ur.getUser()))
                .distinct()
                .toList();
    }

    @Override
    public Optional<CamundaIdentityGroup> findByGroupCode(String code) {
        return roleRepository.findActiveByCode(code)
                .map(this::toGroup);
    }

    @Override
    public List<CamundaIdentityGroup> findAllGroups() {
        return roleRepository.findAllActiveByOrderByRoleId().stream()
                .map(this::toGroup)
                .toList();
    }

    private CamundaIdentityUser toUser(MainUserEntity user) {
        return new CamundaIdentityUser(
                user.getUsername(),
                firstName(user.getNickName()),
                lastName(user.getNickName()),
                user.getEmail()
        );
    }

    private CamundaIdentityGroup toGroup(com.github.zeng.alt.camunda.identity.jpa.entity.MainRoleEntity role) {
        return new CamundaIdentityGroup(role.getCode(), role.getName(), null);
    }

    private String firstName(String nickName) {
        if (nickName == null || nickName.isBlank()) {
            return null;
        }
        int idx = nickName.indexOf(' ');
        return idx > 0 ? nickName.substring(0, idx) : nickName;
    }

    private String lastName(String nickName) {
        if (nickName == null || nickName.isBlank()) {
            return null;
        }
        int idx = nickName.indexOf(' ');
        return idx > 0 && idx < nickName.length() - 1 ? nickName.substring(idx + 1) : null;
    }
}
