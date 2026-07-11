package com.github.zeng.alt.json.spi;

import org.springframework.lang.Nullable;

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
