package com.github.zeng.alt.config.client;

import org.springframework.web.client.RestTemplate;

/**
 * 为组件持有的 HTTP 客户端贡献拦截器等配置，可用 Order 指定顺序。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface ConfigHttpClientCustomizer {
    void customize(RestTemplate client);
}
