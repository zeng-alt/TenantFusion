package com.github.zeng.alt.log.core.support;

import java.util.Map;

/**
 * @author zengJiaJun
 * @since 2026年07月01日
 * @version 1.0
 */
public interface RequestResolver {
    String requestURI();

    String method();

    /** 返回请求参数；非 HTTP 调用默认为空。 */
    default Map<String, String[]> parameters() {
        return Map.of();
    }
}
