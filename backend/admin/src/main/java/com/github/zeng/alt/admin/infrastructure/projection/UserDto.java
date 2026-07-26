package com.github.zeng.alt.admin.infrastructure.projection;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.zeng.alt.admin.infrastructure.entity.Role;
import com.github.zeng.alt.admin.infrastructure.entity.User;
import com.github.zeng.alt.admin.infrastructure.entity.UserRole;
import com.github.zeng.alt.domain.key.SnowflakeId;
import jakarta.persistence.*;
import lombok.Data;

import java.util.LinkedList;
import java.util.List;

/**
 * @author zengJiaJun
 * @since 2026年07月20日
 * @version 1.0
 */
@Data
public class UserDto {

    private Long userId;
    private String username;
    private String nickName;
    private String avatar;
    private String email;
    private String phoneNumber;
    private String gender;
    private String status;           // ACTIVE, LOCKED
    private Boolean enabled;
    private Long deptId;
    private List<UserRoleDto> userRoles = new LinkedList<>();

    @Data
    public static class UserRoleDto {
        private Long userRoleId;
        private RoleDto role;
    }

    @Data
    public static class RoleDto {
        private Long roleId;
        private String code;
        private String name;
        private Integer roleSort;
        private Boolean enabled;
    }
}
