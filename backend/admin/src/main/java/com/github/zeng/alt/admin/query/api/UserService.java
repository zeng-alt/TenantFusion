package com.github.zeng.alt.admin.query.api;

import com.github.zeng.alt.admin.query.api.dto.*;
import io.vavr.control.Either;
import jakarta.validation.Valid;

/**
 * @author zengJiaJun
 * @since 2026年07月14日
 * @version 1.0
 */
public interface UserService {

    CurrentUserDto currentUser();

    void create(CreateUserDto dto);

    Either<String, Long> patchUser(Long id, PatchUserDto dto);

    Either<String, Long> resetPassword(Long id, ResetUserPasswordDto dto);

    Either<String, Long> patchProfile(Long id, PatchProfileDto dto);

    void changePassword(PasswordDto dto);
}
