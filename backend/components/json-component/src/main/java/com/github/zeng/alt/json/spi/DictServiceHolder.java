package com.github.zeng.alt.json.spi;

import org.springframework.lang.Nullable;

/**
 * {@link IDictTranslateService} 的静态持有者。由 {@code JsonConfiguration} 在容器启动时注入，
 * 支持在无实现时优雅降级（序列化器直接输出原始值）。
 */
public final class DictServiceHolder {

    @Nullable
    private static volatile IDictTranslateService service;

    public DictServiceHolder() {
    }

    public static void setService(@Nullable IDictTranslateService service) {
        DictServiceHolder.service = service;
    }

    @Nullable
    public static IDictTranslateService getService() {
        return service;
    }

}
