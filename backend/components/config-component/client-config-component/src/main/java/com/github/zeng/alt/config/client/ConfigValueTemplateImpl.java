package com.github.zeng.alt.config.client;

import com.github.zeng.alt.config.ConfigValueTemplate;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

/**
 * 标量业务配置解析。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class ConfigValueTemplateImpl implements ConfigValueTemplate {
    private final Function<String, Option<String>> valueByKey;

    @Override
    public Option<String> getString(String key) {
        return valueByKey.apply(key);
    }

    @Override
    public String getString(String key, String defaultValue) {
        return getString(key).getOrElse(Objects.requireNonNull(defaultValue, "defaultValue"));
    }

    @Override
    public Either<String, Boolean> getBoolean(String key) {
        return read(key, Option.none(), Boolean.class, this::parseBoolean);
    }

    @Override
    public Either<String, Boolean> getBoolean(String key, boolean defaultValue) {
        return read(key, Option.some(defaultValue), Boolean.class, this::parseBoolean);
    }

    @Override
    public Either<String, Integer> getInteger(String key) {
        return read(key, Option.none(), Integer.class, Integer::valueOf);
    }

    @Override
    public Either<String, Integer> getInteger(String key, int defaultValue) {
        return read(key, Option.some(defaultValue), Integer.class, Integer::valueOf);
    }

    @Override
    public Either<String, Long> getLong(String key) {
        return read(key, Option.none(), Long.class, Long::valueOf);
    }

    @Override
    public Either<String, Long> getLong(String key, long defaultValue) {
        return read(key, Option.some(defaultValue), Long.class, Long::valueOf);
    }

    @Override
    public Either<String, BigDecimal> getDecimal(String key) {
        return read(key, Option.none(), BigDecimal.class, BigDecimal::new);
    }

    @Override
    public Either<String, BigDecimal> getDecimal(String key, BigDecimal defaultValue) {
        return read(
            key,
            Option.some(Objects.requireNonNull(defaultValue, "defaultValue")),
            BigDecimal.class,
            BigDecimal::new
        );
    }

    private Boolean parseBoolean(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException("Expected true or false");
        };
    }

    private <T> Either<String, T> read(String key, Option<T> fallback, Class<T> type, Function<String, T> parser) {
        return getString(key)
                .map(value -> Try.of(() -> parser.apply(value.trim())).toEither()
                        .mapLeft(error -> "配置 " + key + " 无法解析为 " + type.getSimpleName()))
                .getOrElse(() -> fallback.toEither("配置不存在: " + key));
    }
}
