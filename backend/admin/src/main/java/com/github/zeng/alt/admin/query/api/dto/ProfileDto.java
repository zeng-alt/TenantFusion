package com.github.zeng.alt.admin.query.api.dto;

import lombok.Data;

@Data
public class ProfileDto {

    private Long id;

    private String nickName;

    private Integer gender;

    private String avatar;

    private String address;

    private String email;

    private Long userId;
}