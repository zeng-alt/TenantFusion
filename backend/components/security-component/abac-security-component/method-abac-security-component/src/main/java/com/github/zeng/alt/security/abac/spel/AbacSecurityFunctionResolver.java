package com.github.zeng.alt.security.abac.spel;

import com.github.zeng.alt.security.abac.AbacContext;
import com.github.zeng.alt.security.abac.AbacFailure;
import io.vavr.control.Either;

import java.util.List;
import java.util.Set;

/**
 * 使用当前调用认证快照的常用授权函数，角色码保持项目原值。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public class AbacSecurityFunctionResolver implements AbacFunctionResolver {
    @Override
    public Set<String> functions() {
        return Set.of("hasRole", "hasAnyRole", "hasAuthority", "hasAnyAuthority", "isAuthenticated", "isAnonymous");
    }

    @Override
    public Either<AbacFailure, Boolean> evaluate(String name, List<Object> arguments, AbacContext context) {
        final var authentication = context.invocation().authentication();
        final boolean authenticated = Boolean.TRUE.equals(authentication.get("authenticated"));
        if (name.equals("isAuthenticated") || name.equals("isAnonymous")) {
            return arguments.isEmpty() ? Either.right(name.equals("isAuthenticated") == authenticated)
                    : Either.left(new AbacFailure("FUNCTION_ARGUMENTS", "认证函数不接受参数"));
        }
        if (arguments.isEmpty() || arguments.stream().anyMatch(value -> !(value instanceof String))
                || ((name.equals("hasRole") || name.equals("hasAuthority")) && arguments.size() != 1)) {
            return Either.left(new AbacFailure("FUNCTION_ARGUMENTS", "角色/权限函数需要字符串参数"));
        }
        final Object values = authentication.get(name.contains("Role") ? "roles" : "authorities");
        return Either.right(authenticated && values instanceof List<?> available
                && arguments.stream().anyMatch(available::contains));
    }
}
