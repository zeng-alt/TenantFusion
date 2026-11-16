package com.github.zeng.alt.config;

import io.vavr.control.Either;
import io.vavr.control.Option;
import java.math.BigDecimal;

/**
 * 标量配置读取。默认值仅用于缺失的配置，格式错误始终返回 Left。
 * 布尔值只接受 true / false，忽略大小写和首尾空白。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public interface ConfigValueTemplate {
    Option<String> getString(String key);

    String getString(String key, String defaultValue);

    Either<String, Boolean> getBoolean(String key);

    Either<String, Boolean> getBoolean(String key, boolean defaultValue);

    Either<String, Integer> getInteger(String key);

    Either<String, Integer> getInteger(String key, int defaultValue);

    Either<String, Long> getLong(String key);

    Either<String, Long> getLong(String key, long defaultValue);

    Either<String, BigDecimal> getDecimal(String key);

    Either<String, BigDecimal> getDecimal(String key, BigDecimal defaultValue);
}
