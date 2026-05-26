package com.github.zeng.alt.api.rest;

import com.github.zeng.alt.api.base.BaseEnum;
import lombok.Getter;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2026年05月26日
 */
@Getter
public enum ResponseEnum implements BaseEnum {

    SUCCESS(200, "success"),
    FAIL(500, "fail"),
    WARN(601, "warn");

    private final Integer code;
    private final String message;

    ResponseEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

}
