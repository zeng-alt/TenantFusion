package com.github.zeng.alt.core.validation;


import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2025年03月27日 17:16
 */
@Component
public class LocalTimeTypeJsonConversionStrategy extends JsonConversionStrategy {


    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public @NonNull <T> T convert(String jsonString) throws Exception {
        return (T) LocalTime.parse(jsonString, FORMATTER);
    }

    @Override
    public @NonNull String getType() {
        return TargetType.LOCALTIME.getType();
    }
}
