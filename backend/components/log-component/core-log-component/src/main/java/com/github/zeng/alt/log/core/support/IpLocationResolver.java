package com.github.zeng.alt.log.core.support;

import io.vavr.control.Option;

/**
 * 根据 IP 查询归属地，不可定位时返回空值。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface IpLocationResolver {
    Option<String> resolve(String ip);
}
