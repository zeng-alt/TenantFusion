package com.github.zeng.alt.log.core.support;

import com.github.zeng.alt.log.core.model.LoginClientDto;

/**
 * 根据 User-Agent 解析登录客户端，缺失或未知时返回空字段。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface UserAgentResolver {
    LoginClientDto resolve(String userAgent);
}
