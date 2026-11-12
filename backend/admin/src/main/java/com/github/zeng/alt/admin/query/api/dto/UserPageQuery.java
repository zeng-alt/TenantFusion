package com.github.zeng.alt.admin.query.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 用户分页请求，未知类型由 MVC 绑定边界校验。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Data
@Accessors(chain = true)
public class UserPageQuery {
    private String username;
    private String gender;
    private Long deptId;
    private Boolean enabled;
    @Min(1)
    private int pageNo = 1;
    @Min(1)
    @Max(100)
    private int pageSize = 10;
    @Pattern(regexp = "id|userId|username|nickName|gender|deptId|enabled|createdDate")
    private String sort = "id";
    @Pattern(regexp = "(?i)asc|desc")
    private String order = "desc";
}
