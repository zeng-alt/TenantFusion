package com.github.zeng.alt.security.abac;

import io.vavr.control.Either;

import java.util.function.Function;

/**
 * 单次授权请求；每次 get 都读取当前策略，不缓存最终授权结果。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class AbacAuthorizationBuilder {
    private final Function<Boolean, Either<AbacFailure, Boolean>> evaluation;
    private boolean required;

    /** 供独立模块中的 AbacTemplate 实现创建请求对象；evaluation 必须完成实际授权检查。 */
    public AbacAuthorizationBuilder(Function<Boolean, Either<AbacFailure, Boolean>> evaluation) {
        this.evaluation = evaluation;
    }

    public AbacAuthorizationBuilder requirePolicy() {
        required = true;
        return this;
    }

    public Either<AbacFailure, Boolean> get() {
        return evaluation.apply(required);
    }
}
