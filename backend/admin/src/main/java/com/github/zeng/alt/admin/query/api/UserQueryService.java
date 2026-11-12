package com.github.zeng.alt.admin.query.api;

import app.tozzi.model.input.JPASearchInput;
import com.github.zeng.alt.admin.infrastructure.entity.User;
import com.github.zeng.alt.admin.infrastructure.projection.UserDto;
import com.github.zeng.alt.admin.query.api.dto.UserInfoDto;
import com.github.zeng.alt.admin.query.api.dto.UserPageQuery;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.springframework.data.domain.Page;

/**
 * 用户业务读取入口；账号认证和写操作仍使用内部仓储。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public interface UserQueryService {
    Either<String, Page<UserDto>> page(UserPageQuery query);
    Either<String, Option<User>> getDetail(Long id);
    Either<String, Option<UserInfoDto>> getInfo(Long id, String username);
    Either<String, Page<UserDto>> search(JPASearchInput input);
}
