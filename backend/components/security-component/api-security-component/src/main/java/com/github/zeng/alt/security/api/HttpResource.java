package com.github.zeng.alt.security.api;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年10月09日 22:50
 */
public class HttpResource extends AbstractResource {

    @Override
    public String getKey() {
        return getUri() + ":" + getMethod();
    }
}
