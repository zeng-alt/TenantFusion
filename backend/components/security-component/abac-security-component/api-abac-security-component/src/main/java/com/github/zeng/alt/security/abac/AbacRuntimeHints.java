package com.github.zeng.alt.security.abac;

import com.github.zeng.alt.security.abac.event.AbacPolicyChangedEvent;
import com.github.zeng.alt.security.abac.policy.AbacPolicyDto;
import com.github.zeng.alt.security.abac.policy.AbacPolicyQuery;
import com.github.zeng.alt.security.abac.policy.AbacPolicySnapshotDto;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * HTTP 和事件协议的 Native 序列化可达性，不注册或传输 SpEL 执行对象。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public class AbacRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        new BindingReflectionHintsRegistrar().registerReflectionHints(hints.reflection(),
                AbacPolicyQuery.class, AbacPolicyDto.class, AbacPolicySnapshotDto.class, AbacPolicyChangedEvent.class);
    }
}
