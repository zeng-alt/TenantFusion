package com.github.zeng.alt.json.spi;

import org.springframework.lang.Nullable;

/**
 * @author zengJiaJun
 * @since 2026年07月11日
 * @version 1.0
 */
public final class EncryptServiceHolder {

    @Nullable
    private static volatile IEncryptService service;

    public EncryptServiceHolder() {
    }

    public static void setService(@Nullable IEncryptService service) {
        EncryptServiceHolder.service = service;
    }

    @Nullable
    public static IEncryptService getService() {
        return service;
    }

}
