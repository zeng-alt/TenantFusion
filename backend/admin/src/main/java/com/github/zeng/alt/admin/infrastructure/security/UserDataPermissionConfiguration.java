package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.infrastructure.entity.User;
import com.github.zeng.alt.admin.infrastructure.projection.UserDto;
import com.github.zeng.alt.admin.query.api.dto.UserInfoDto;
import com.github.zeng.alt.admin.query.api.dto.UserPageQuery;
import com.github.zeng.alt.admin.query.api.dto.UserSearchQuery;
import com.github.zeng.alt.api.rest.PageEntity;
import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.security.data.core.DataPermissionCustomizer;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 仅为用户资源贡献数据权限映射，其他资源保持原有查询行为。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Configuration(proxyBeanMethods = false)
@RegisterReflectionForBinding({User.class, UserDto.class, UserInfoDto.class, UserPageQuery.class,
        UserSearchQuery.class, PageEntity.class, PageRestResponse.class, RestResponse.class})
public class UserDataPermissionConfiguration {
    @Bean
    public DataPermissionCustomizer userDataPermissionCustomizer() {
        return properties -> properties.resource("user", User.class,
                fields -> fields.setTenant("tenantBy").setOwner("userId").setDepartment("deptId"));
    }
}
