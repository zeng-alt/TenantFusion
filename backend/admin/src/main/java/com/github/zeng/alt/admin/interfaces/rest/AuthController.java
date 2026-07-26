package com.github.zeng.alt.admin.interfaces.rest;

import com.github.zeng.alt.admin.query.api.RoleService;
import com.github.zeng.alt.admin.query.api.UserService;
import com.github.zeng.alt.admin.query.api.dto.PasswordDto;
import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.captcha.core.CaptchaTemplate;
import com.github.zeng.alt.security.api.LoginHelper;
import com.github.zeng.alt.security.api.LoginResponse;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.security.captcha.CaptchaAuthProperties;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Tag(name = "认证接口")
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CaptchaTemplate captchaTemplate;
    private final CaptchaAuthProperties captchaAuthProperties;
    private final SecurityProperties securityProperties;
    private final LoginHelper loginHelper;
    private final UserService userService;
    private final RoleService roleService;

    @GetMapping("/captcha")
    @Operation(summary = "获取验证码")
    public void captcha(HttpServletResponse response) throws IOException {
        captchaTemplate.generate()
                .cookie(response, captchaAuthProperties.getCookieName())
                .writeTo(response);
    }


    @Operation(summary = "切换角色")
    @PostMapping("/current-role/switch/{code}/{rememberMe}")
    public RestResponse<Map<String, Object>> switchRole(@PathVariable String code, @PathVariable boolean rememberMe, HttpServletRequest request, HttpServletResponse response) {
        SecurityUser securityUser = UserContextHolder.getSecurityUser();
        if (securityUser == null) {
            RestResponse<Map<String, Object>> status = RestResponse.status(401);
            return status.message("请重新登录");
        }
        Optional<String> optional = roleService.getRoleCodes().stream().filter(code::equalsIgnoreCase).findAny();
        if (optional.isEmpty()) {
            RestResponse<Map<String, Object>> fail = RestResponse.fail();
            return fail.message("当前用户没有["+code+"]角色");
        }
        securityUser.setCurrentRole(new SimpleGrantedAuthority(code));
        LoginResponse reset = loginHelper.reset(securityUser, rememberMe, request, response);
        loginHelper.logout(request);
        return RestResponse.success(reset.getAttributes());
    }

    @Operation(summary = "获取默认内置的用户和角色")
    @GetMapping("/admin")
    public RestResponse<SecurityProperties.AdminRole> admin() {
        return RestResponse.success(securityProperties.getAdmin());
    }


    @Operation(summary = "修改自己的密码")
    @PostMapping("/password")
    public RestResponse<Void> password(@Valid @RequestBody PasswordDto dto) {
        userService.changePassword(dto);
        return RestResponse.success();
    }

    @GetMapping("/logoff/{id}")
    public RestResponse<Void> logoff(@PathVariable Long id) {
        loginHelper.logout(id);
        return RestResponse.success();
    }
}
