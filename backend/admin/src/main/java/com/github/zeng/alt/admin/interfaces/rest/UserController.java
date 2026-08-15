package com.github.zeng.alt.admin.interfaces.rest;

import com.github.zeng.alt.admin.query.api.UserService;
import com.github.zeng.alt.admin.query.api.dto.*;
import com.github.zeng.alt.api.rest.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @author zengJiaJun
 * @since 2026年07月14日
 * @version 1.0
 */
@Tag(name = "用户接口")
@RestController
@RequestMapping("/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/detail")
    public RestResponse<CurrentUserDto> currentUser() {
        return RestResponse.success(userService.currentUser());
    }

    @Operation(summary = "获取用户基础信息（按userId或username）")
    @GetMapping("/info")
    public RestResponse<UserInfoDto> userInfo(@RequestParam(required = false) Long userId,
                                              @RequestParam(required = false) String username) {
        return RestResponse.success(userService.userInfo(userId, username));
    }

    @Operation(summary = "新增后台用户")
    @PostMapping
    public RestResponse<Void> create(@Valid @RequestBody CreateUserDto dto) {
        userService.create(dto);
        return RestResponse.success();
    }

    @Operation(summary = "修改后台用户")
    @PatchMapping("/{id}")
    public RestResponse<?> patchUser(@PathVariable Long id, @Valid @RequestBody PatchUserDto dto) {
        return userService.patchUser(id, dto).fold(RestResponse::fail, RestResponse::success);
    }

    @Operation(summary = "修改用户密码")
    @PatchMapping("/password/reset/{id}")
    public RestResponse<?> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetUserPasswordDto dto) {
        return userService.resetPassword(id, dto).fold(RestResponse::fail, RestResponse::success);
    }

    @Operation(summary = "修改自己的用户信息")
    @PatchMapping("/profile/{id}")
    public RestResponse<?> patchProfile(@PathVariable Long id, @Valid @RequestBody PatchProfileDto dto) {
        return userService.patchProfile(id, dto).fold(RestResponse::fail, RestResponse::success);
    }
}
