package com.github.zeng.alt.security.abac;

import io.vavr.control.Option;

import java.util.Map;

/**
 * 一次调用的阶段及只读数据，参数和返回值均不进入全局缓存。
 *
 * @param phase 执行阶段
 * @param authentication 认证快照
 * @param variables 按名称及位置别名绑定的方法参数
 * @param returnObject 成功返回值的投影
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record AbacInvocationDto(
    AbacPhaseDto phase,
    Map<String, Object> authentication,
    Map<String, Object> variables,
    Option<Object> returnObject
) {
    public AbacInvocationDto {
        authentication = AbacContext.snapshot(authentication);
        variables = AbacContext.snapshot(variables);
        returnObject = returnObject.map(AbacContext::snapshotValue);
    }

    public boolean preAuth() { return phase.preAuth(); }
    public boolean transactionActive() { return phase.transactionActive(); }

    public static AbacInvocationDto method(Map<String, Object> authentication,
            Map<String, Object> variables, boolean transactionActive) {
        return new AbacInvocationDto(new AbacPhaseDto(true, transactionActive),
                authentication, variables, Option.none());
    }

    public AbacInvocationDto after(Option<Object> value, boolean transactionActive) {
        return new AbacInvocationDto(new AbacPhaseDto(false, transactionActive), authentication, variables, value);
    }
}
