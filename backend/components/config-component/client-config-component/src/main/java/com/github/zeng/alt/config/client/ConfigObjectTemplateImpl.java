package com.github.zeng.alt.config.client;

import tools.jackson.databind.ObjectMapper;
import com.github.zeng.alt.config.ConfigObjectTemplate;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.util.Objects;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

/**
 * JSON 业务配置对象绑定。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class ConfigObjectTemplateImpl implements ConfigObjectTemplate {
    private final Function<String, Option<String>> valueByKey;
    private final ObjectMapper mapper;

    @Override
    public <T> Either<String, T> get(String key, Class<T> type) {
        Objects.requireNonNull(type, "type");
        return valueByKey.apply(key).toEither("配置不存在: " + key)
                .flatMap(content -> Try.of(() -> mapper.readValue(content, type)).toEither()
                        .mapLeft(error -> "配置 " + key + " 无法绑定为 " + type.getSimpleName()))
                .flatMap(value -> Option.of(value).toEither("配置 " + key + " 不能为 null"));
    }
}
