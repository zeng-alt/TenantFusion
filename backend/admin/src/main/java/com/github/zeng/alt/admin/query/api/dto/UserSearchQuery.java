package com.github.zeng.alt.admin.query.api.dto;

import app.tozzi.annotation.Searchable;
import lombok.Data;

/**
 * 高级搜索字段白名单，不开放口令、关联实体或租户字段。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Data
public class UserSearchQuery {
    @Searchable
    private Long userId;
    @Searchable
    private String username;
    @Searchable
    private String gender;
    @Searchable
    private Long deptId;
    @Searchable
    private Boolean enabled;
}
