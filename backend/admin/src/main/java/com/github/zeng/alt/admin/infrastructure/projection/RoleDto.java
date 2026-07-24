package com.github.zeng.alt.admin.infrastructure.projection;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author zengJiaJun
 * @since 2026年07月20日
 * @version 1.0
 */
@Data
public class RoleDto {

    private Long roleId;
    private String code;
    private String name;
    private Integer roleSort = 0;
    private Boolean enabled = true;

    @JsonIgnore
    private Set<RolePermissionDto> rolePermissions = new HashSet<>();

    public Set<Long> getPermissionIds() {
        return rolePermissions.stream().map(RolePermissionDto::getPermission).map(PermissionDto::getPermissionId).collect(Collectors.toSet());
    }

    @Data
    public static class RolePermissionDto {
        private Long rolePermissionId;
        private PermissionDto permission;
    }

    @Data
    public static class PermissionDto {
        private Long permissionId;
    }
}
