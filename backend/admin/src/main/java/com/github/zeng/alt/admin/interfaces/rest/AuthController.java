package com.github.zeng.alt.admin.interfaces.rest;

import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.captcha.core.CaptchaTemplate;
import com.github.zeng.alt.captcha.model.CaptchaInfo;
import com.github.zeng.alt.security.api.LoginHelper;
import com.github.zeng.alt.security.api.LoginResponse;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.security.captcha.CaptchaAuthProperties;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import com.github.zeng.alt.security.jwt.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CaptchaTemplate captchaTemplate;
    private final CaptchaAuthProperties captchaAuthProperties;
    private final SecurityProperties securityProperties;
    private final LoginHelper loginHelper;

    @GetMapping("/captcha")
    public void captcha(HttpServletResponse response) throws IOException {
        CaptchaInfo captchaInfo = captchaTemplate.generate();

        Cookie cookie = new Cookie(captchaAuthProperties.getCookieName(), captchaInfo.getKey());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) captchaInfo.getExpireIn());
        response.addCookie(cookie);

        String base64Data = captchaInfo.getImageBase64();
        base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
        byte[] imageBytes = Base64.getDecoder().decode(base64Data);

        response.setContentType("image/png");
        response.setCharacterEncoding("utf-8");
        response.getOutputStream().write(imageBytes);
        response.getOutputStream().flush();
    }


    @PostMapping("/current-role/switch/{code}/{rememberMe}")
    public RestResponse<Map<String, Object>> switchRole(@PathVariable String code, @PathVariable boolean rememberMe, HttpServletRequest request, HttpServletResponse response) {
        SecurityUser securityUser = UserContextHolder.getSecurityUser();
        if (securityUser == null) {
            RestResponse<Map<String, Object>> status = RestResponse.status(401);
            return status.title("请重新登录");
        }
        Optional<String> optional = securityUser.getRoles().stream().map(GrantedAuthority::getAuthority).filter(code::equalsIgnoreCase).findAny();
        if (optional.isEmpty()) {
            return RestResponse.fail("当前用户没有["+code+"]角色");
        }
        securityUser.setCurrentRole(new SimpleGrantedAuthority(code));
        LoginResponse reset = loginHelper.reset(securityUser, rememberMe, request, response);
        loginHelper.logout(request);
        return RestResponse.success(reset.getAttributes());
    }

    @GetMapping("/admin")
    public RestResponse<SecurityProperties.AdminRole> admin() {
        return RestResponse.success(securityProperties.getAdmin());
    }
}
