package com.github.zeng.alt.admin.query.api.dto;

import com.github.zeng.alt.admin.infrastructure.HttpResourceDetila;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CurrentUserDto {

    private Long id;

    private String username;

    private Boolean enabled;

    private LocalDateTime createdDate;

    private LocalDateTime lastModifiedDate;

    private ProfileDto profile;

    private List<RoleDto> roles;

    private RoleDto currentRole;
}