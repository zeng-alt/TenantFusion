package com.github.zeng.alt.admin.interfaces.rest;

import com.github.zeng.alt.admin.query.api.UserService;
import com.github.zeng.alt.admin.query.api.dto.CurrentUserDto;
import com.github.zeng.alt.api.rest.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
