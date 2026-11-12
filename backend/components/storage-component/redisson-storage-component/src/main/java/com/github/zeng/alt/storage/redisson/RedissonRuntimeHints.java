package com.github.zeng.alt.storage.redisson;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

/**
 * 补齐 Netty 4.2.19 在 GraalVM 25 下的非对齐访问检测元数据。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public class RedissonRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // 上游元数据条件仍引用 PlatformDependent0$6，4.2.19 已改为 $5，导致此字段未注册。
        hints.reflection().registerType(TypeReference.of("java.nio.Bits"), type -> type.withField("UNALIGNED"));
    }
}
