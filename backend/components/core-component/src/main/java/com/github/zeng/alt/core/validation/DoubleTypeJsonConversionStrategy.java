package com.github.zeng.alt.core.validation;


import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2025年03月27日 17:16
 */
@Component
public class DoubleTypeJsonConversionStrategy extends JsonConversionStrategy {

    @Override
    public @NonNull <T> T convert(String jsonString) throws Exception {
        return (T) Double.valueOf(jsonString);
    }

    @Override
    public @NonNull String getType() {
        return TargetType.DOUBLE.getType();
    }
}
