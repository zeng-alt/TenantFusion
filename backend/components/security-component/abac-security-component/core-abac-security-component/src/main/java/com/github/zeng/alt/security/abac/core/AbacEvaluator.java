package com.github.zeng.alt.security.abac.core;

import com.github.zeng.alt.security.abac.AbacContext;
import com.github.zeng.alt.security.abac.AbacFailure;
import io.vavr.control.Either;

/**
 * 策略条件求值 SPI；实现负责限制可执行能力，将解析、属性缺失及求值失败返回为 Left。
 * 核心授权流程不依赖任何具体表达式引擎。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface AbacEvaluator {
    /** 在规则进入执行缓存前准备表达式；不依赖表达式解析的实现可直接接受。 */
    default Either<AbacFailure, Boolean> prepare(String condition) {
        return Either.right(true);
    }

    Either<AbacFailure, Boolean> evaluate(String condition, AbacContext context);
}
