package com.github.zeng.alt.config;

import io.vavr.control.Either;

/**
 * 将 JSON 配置绑定为业务类型；不存在、格式错误和 JSON null 均返回 Left。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public interface ConfigObjectTemplate {
    <T> Either<String, T> get(String key, Class<T> type);
}
