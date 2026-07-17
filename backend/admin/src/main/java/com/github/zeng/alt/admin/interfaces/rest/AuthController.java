package com.github.zeng.alt.admin.interfaces.rest;

import com.github.zeng.alt.admin.infrastructure.repository.UserRepository;
import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.captcha.core.CaptchaTemplate;
import com.github.zeng.alt.captcha.model.CaptchaInfo;
import com.github.zeng.alt.security.captcha.CaptchaAuthProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CaptchaTemplate captchaTemplate;
    private final CaptchaAuthProperties captchaAuthProperties;

    @GetMapping("/captcha")
    public RestResponse<CaptchaInfo> captcha(HttpServletResponse response) {
        CaptchaInfo captchaInfo = captchaTemplate.generate();

        Cookie cookie = new Cookie(captchaAuthProperties.getCookieName(), captchaInfo.getKey());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) captchaInfo.getExpireIn());
        response.addCookie(cookie);

        captchaInfo.setKey(null);
        return RestResponse.success(captchaInfo);
    }
}
