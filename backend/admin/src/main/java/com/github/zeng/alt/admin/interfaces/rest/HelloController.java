package com.github.zeng.alt.admin.interfaces.rest;

import com.github.zeng.alt.api.rest.RestResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zengJiaJun
 * @since 2026年06月27日
 * @version 1.0
 */
@RestController
@RequestMapping("/hello")
public class HelloController {

    @GetMapping
    public RestResponse<String> hello() {
        return RestResponse.success("hello").message("成功");
    }
}
