package com.github.zeng.alt.admin.interfaces.rest;

import com.github.zeng.alt.captcha.core.CaptchaTemplate;
import com.github.zeng.alt.captcha.model.CaptchaInfo;
import com.github.zeng.alt.security.captcha.CaptchaAuthProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Base64;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CaptchaTemplate captchaTemplate;
    private final CaptchaAuthProperties captchaAuthProperties;

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
}
