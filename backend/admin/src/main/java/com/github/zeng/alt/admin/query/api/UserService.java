package com.github.zeng.alt.admin.query.api;

import com.github.zeng.alt.admin.infrastructure.entity.User;
import com.github.zeng.alt.admin.query.api.dto.CreateUserDto;
import com.github.zeng.alt.admin.query.api.dto.CurrentUserDto;
import com.github.zeng.alt.admin.query.api.dto.PatchUserDto;
import com.github.zeng.alt.admin.query.api.dto.ResetUserPasswordDto;
import io.vavr.control.Either;

/**
 * @author zengJiaJun
 * @since 2026年07月14日
 * @version 1.0
 */
public interface UserService {

    public CurrentUserDto currentUser();

    void create(CreateUserDto dto);

    Either<String, Long> patchUser(Long id, PatchUserDto dto);

    Either<String, Long> resetPassword(Long id, ResetUserPasswordDto dto);
}
