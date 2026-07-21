package com.github.zeng.alt.admin.infrastructure.projection;

import com.github.zeng.alt.bean.ApplicationContextHelper;
import lombok.Data;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import java.util.LinkedList;
import java.util.List;

/**
 * @author zengJiaJun
 * @since 2026年07月20日
 * @version 1.0
 */
@Data
public class StoreUserDto {

    private String username;
    private Boolean enabled = true;
    private String password;
    private List<UserRoleDto> userRoles = new LinkedList<>();

    public void setRoleIds(List<Long> ids) {
        userRoles = ids.stream().map(id -> {
            UserRoleDto userRoleDto = new UserRoleDto();
            RoleDto roleDto = new RoleDto();
            roleDto.setRoleId(id);
            userRoleDto.setRole(roleDto);
            return userRoleDto;
        }).toList();
    }

    public void setPassword(String password) {
        if (StringUtils.hasText(password)) {
            return;
        }
        this.password = ApplicationContextHelper.getBean(PasswordEncoder.class).encode(password);
    }

    @Data
    public static class UserRoleDto {
        private RoleDto role;
    }

    @Data
    public static class RoleDto {
        private Long roleId;
    }
}
