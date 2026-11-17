package com.github.zeng.alt.log.ip2region;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * 为放在标准 classpath 目录下的离线 xdb 注册资源提示。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class Ip2RegionRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerPattern("ip2region/*.xdb");
    }
}
