package com.github.zeng.alt.core.validation;


import com.github.zeng.alt.json.JacksonHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;

import java.util.List;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2025年03月27日 17:16
 */
@RequiredArgsConstructor
public class ListTypeJsonConversionStrategy extends JsonConversionStrategy {

    private final JacksonHelper jsonHelper;

    @Override
    public @NonNull <T> T convert(String jsonString) throws Exception {
        return (T) jsonHelper.parseObject(jsonString, List.class);
    }

    @Override
    public @NonNull String getType() {
        return TargetType.LIST.getType();
    }
}
