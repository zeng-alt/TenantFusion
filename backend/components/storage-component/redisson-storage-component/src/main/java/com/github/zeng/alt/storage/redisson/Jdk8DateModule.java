package com.github.zeng.alt.storage.redisson;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年10月16日 16:41
 */
public class Jdk8DateModule extends Module {
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
        context.setMixInAnnotations(LocalDateTime.class, UseTypeInfo.class);
        context.setMixInAnnotations(LocalDate.class, UseTypeInfo.class);
        context.setMixInAnnotations(LocalTime.class, UseTypeInfo.class);
    }
}
