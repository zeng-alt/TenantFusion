package com.github.zeng.alt.admin.interfaces.rest;

import com.github.zeng.alt.admin.infrastructure.entity.Role;
import com.github.zeng.alt.admin.infrastructure.entity.Tenant;
import com.github.zeng.alt.admin.infrastructure.entity.User;
import com.github.zeng.alt.admin.infrastructure.entity.UserRole;
import com.github.zeng.alt.admin.infrastructure.repository.RoleRepository;
import com.github.zeng.alt.admin.infrastructure.repository.TenantRepository;
import com.github.zeng.alt.admin.infrastructure.repository.UserRepository;
import com.github.zeng.alt.admin.infrastructure.repository.UserRoleRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 为 workflow 的 Camunda 身份提供者暴露用户/角色查询接口。
 * <p>
 * 默认仅返回启用且未删除的用户；密码校验由 admin 本地完成，不返回密码。
 */
@Tag(name = "Camunda 身份接口")
@RestController
@RequestMapping("/v1/camunda-identity")
@RequiredArgsConstructor
public class CamundaIdentityController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/users/{username}")
    public CamundaIdentityUser getUser(@PathVariable String username) {
        User user = userRepository.findByUsername(username)
                .filter(u -> Boolean.TRUE.equals(u.getEnabled()) && !Boolean.TRUE.equals(u.getDeleted()))
                .orElse(null);
        if (user == null) {
            return null;
        }
        List<CamundaIdentityGroup> groups = userRoleRepository.findByUserId(user.getUserId()).stream()
                .map(UserRole::getRole)
                .filter(r -> Boolean.TRUE.equals(r.getEnabled()))
                .map(this::toGroup)
                .toList();
        return toUser(user, groups);
    }

    @PostMapping("/users/{username}/password")
    public boolean validatePassword(@PathVariable String username, @RequestBody PasswordCheckRequest request) {
        return userRepository.findByUsername(username)
                .filter(u -> Boolean.TRUE.equals(u.getEnabled()) && !Boolean.TRUE.equals(u.getDeleted()))
                .map(u -> passwordEncoder.matches(request.getPassword(), u.getPassword()))
                .orElse(false);
    }

    @GetMapping("/groups")
    public List<CamundaIdentityGroup> listGroups() {
        return roleRepository.findAll().stream()
                .filter(r -> Boolean.TRUE.equals(r.getEnabled()))
                .map(this::toGroup)
                .toList();
    }

    @GetMapping("/tenants")
    public List<CamundaIdentityTenant> listTenants(@RequestParam(required = false) String username) {
        if (username != null && !username.isBlank()) {
            return userRepository.findByUsername(username)
                    .filter(u -> Boolean.TRUE.equals(u.getEnabled()) && !Boolean.TRUE.equals(u.getDeleted()))
                    .map(u -> tenantRepository.findActiveById(u.getTenantBy())
                            .map(List::of)
                            .orElseGet(List::of))
                    .map(tenants -> tenants.stream().map(this::toTenant).toList())
                    .orElseGet(List::of);
        }
        return tenantRepository.findAllActive().stream()
                .map(this::toTenant)
                .toList();
    }

    @GetMapping("/tenants/{tenantId}")
    public CamundaIdentityTenant getTenant(@PathVariable String tenantId) {
        return tenantRepository.findActiveById(tenantId)
                .map(this::toTenant)
                .orElse(null);
    }

    private CamundaIdentityUser toUser(User user, List<CamundaIdentityGroup> groups) {
        return new CamundaIdentityUser(
                user.getUsername(),
                firstName(user.getNickName()),
                lastName(user.getNickName()),
                user.getEmail(),
                groups
        );
    }

    private CamundaIdentityGroup toGroup(Role role) {
        return new CamundaIdentityGroup(role.getCode(), role.getName(), null);
    }

    private CamundaIdentityTenant toTenant(Tenant tenant) {
        return new CamundaIdentityTenant(tenant.getTenantId(), tenant.getTenantName());
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

    @Data
    public static class PasswordCheckRequest {
        private String password;
    }

    public record CamundaIdentityUser(String id, String firstName, String lastName, String email,
                                      List<CamundaIdentityGroup> groups) {
    }

    public record CamundaIdentityGroup(String id, String name, String type) {
    }

    public record CamundaIdentityTenant(String id, String name) {
    }
}
