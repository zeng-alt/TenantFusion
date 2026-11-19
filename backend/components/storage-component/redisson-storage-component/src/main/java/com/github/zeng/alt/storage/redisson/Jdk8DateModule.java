package com.github.zeng.alt.storage.redisson;

import tools.jackson.core.Version;
import tools.jackson.databind.JacksonModule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年10月16日 16:41
 */
public class Jdk8DateModule extends JacksonModule {
    @Override
    public String getModuleName() {
        return "Jdk8DateModule";
    }

    @Override
    public Version version() {
        return Version.unknownVersion();
    }

    @Override
    public void setupModule(SetupContext context) {
        context.setMixIn(LocalDateTime.class, UseTypeInfo.class);
        context.setMixIn(LocalDate.class, UseTypeInfo.class);
        context.setMixIn(LocalTime.class, UseTypeInfo.class);
    }
}
