package com.github.zeng.alt.admin.query.api.dto;

import lombok.Data;

/**
 * 用户基础信息（用户卡片展示用）
 *
 * @author zengJiaJun
 * @since 2026年08月15日
 * @version 1.0
 */
@Data
public class UserInfoDto {

    private Long userId;

    private String username;

    private String nickName;

    private String avatar;

    private String phoneNumber;

    private String email;

    private Long deptId;

    private String deptName;
}
