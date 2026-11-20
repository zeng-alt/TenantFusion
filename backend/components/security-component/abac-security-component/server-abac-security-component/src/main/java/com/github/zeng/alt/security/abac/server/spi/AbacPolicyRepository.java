package com.github.zeng.alt.security.abac.server.spi;

import com.github.zeng.alt.security.abac.AbacPolicy;
import com.github.zeng.alt.security.abac.AbacFailure;
import com.github.zeng.alt.security.abac.policy.AbacPolicyQuery;
import io.vavr.control.Either;

import java.util.List;

/**
 * 服务端权威策略读取 SPI，由宿主在数据租户边界内实现，不得把查询失败转换为空集合。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface AbacPolicyRepository {
    Either<AbacFailure, List<AbacPolicy>> list(AbacPolicyQuery query);
}
