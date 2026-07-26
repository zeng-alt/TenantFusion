package com.github.zeng.alt.admin.query.api.dto;

import lombok.Data;

/**
 * @author zengJiaJun
 * @since 2026年07月24日
 * @version 1.0
 */
@Data
public class PatchProfileDto {
    private Long id;
    private String address;
    private String email;
    private String gender;
    private String nickName;
}
