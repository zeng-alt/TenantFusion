package com.github.zeng.alt.security.core.properties;

import com.github.zeng.alt.security.api.WhiteListRule;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * HTTP 公开访问与登录访问白名单。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Data
@ConfigurationProperties("security.whitelist")
public class HttpWhiteListProperties {
    private List<WhiteListRule> permitAll = List.of();
    private List<WhiteListRule> authenticated = List.of();
}
