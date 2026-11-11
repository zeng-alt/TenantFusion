package com.github.zeng.alt.security.abac.method;

import com.github.zeng.alt.security.abac.AbacMethodContext;

import java.util.Map;

/**
 * 全局环境贡献 SPI（宿主实现一个）：对所有 @AbacAuthorize 检查贡献环境标量属性。
 * 返回不可变或新建 Map；不得返回 null。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface AbacEnvironmentProvider {
    Map<String, Object> environment(AbacMethodContext context);
}