package com.github.zeng.alt.security.core;

import com.github.zeng.alt.security.api.WhiteListRule;
import com.github.zeng.alt.security.api.WhiteListService;
import com.github.zeng.alt.security.core.properties.HttpWhiteListProperties;
import com.github.zeng.alt.security.core.properties.WhiteListProperties;
import lombok.RequiredArgsConstructor;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 合并新配置与旧版公开路径的白名单规则源。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class WhiteListServiceImpl implements WhiteListService {
    private final WhiteListProperties legacy;
    private final HttpWhiteListProperties properties;

    @Override
    public Set<String> getWhiteList() {
        return Set.copyOf(legacy.getIgnoreUrl());
    }

    @Override
    public Set<WhiteListRule> getPermitAll() {
        return Stream.concat(WhiteListService.super.getPermitAll().stream(), properties.getPermitAll().stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<WhiteListRule> getAuthenticated() {
        return Set.copyOf(properties.getAuthenticated());
    }
}
