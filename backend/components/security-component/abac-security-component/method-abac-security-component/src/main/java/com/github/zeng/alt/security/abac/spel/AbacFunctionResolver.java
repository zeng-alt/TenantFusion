package com.github.zeng.alt.security.abac.spel;

import com.github.zeng.alt.security.abac.AbacContext;
import com.github.zeng.alt.security.abac.AbacFailure;
import io.vavr.control.Either;

import java.util.List;
import java.util.Set;

/**
 * 可贡献的授权函数；注册名称对应只读调用，不暴露 Spring Bean 查找。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public interface AbacFunctionResolver {
    Set<String> functions();
    Either<AbacFailure, Boolean> evaluate(String name, List<Object> arguments, AbacContext context);
}
