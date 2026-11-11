package com.github.zeng.alt.security.abac.method;

import io.vavr.control.Either;

/**
 * 主体解析 SPI（宿主实现一个）：完成认证、租户一致性、RBAC 预检并加载可信主体属性。
 * 预期内失败返回 Left（用户可见消息），不得吞失败为放行。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface AbacSubjectProvider {
    Either<String, AbacSubject> resolve(String permission);
}