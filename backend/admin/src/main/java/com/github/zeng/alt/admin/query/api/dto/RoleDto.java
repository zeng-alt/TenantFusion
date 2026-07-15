package com.github.zeng.alt.admin.query.api.dto;

import lombok.Data;

@Data
public class RoleDto {

    private Long id;

    private String code;

    private String name;

    private Boolean enabled;
}