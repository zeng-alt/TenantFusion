package com.github.zeng.alt.security.abac.core;

import com.github.zeng.alt.security.abac.AbacContext;
import com.github.zeng.alt.security.abac.AbacPolicy;

import java.util.List;

/**
 * 按租户和权限读取策略快照。宿主负责存储访问，不能把加载失败返回为空列表。
 * 返回该权限的全部规则，由组件统一处理资源范围和预授权限制。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface AbacPolicyLoader {
    List<AbacPolicy> list(AbacContext context);
}
