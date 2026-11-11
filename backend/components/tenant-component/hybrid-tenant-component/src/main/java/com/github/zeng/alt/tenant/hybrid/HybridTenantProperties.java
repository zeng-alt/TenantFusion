package com.github.zeng.alt.tenant.hybrid;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 混合模式配置。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
@Data
@ConfigurationProperties(prefix = "alt.tenant.hybrid")
public class HybridTenantProperties {

    /**
     * 租户元数据不可用时（远端源网络中断、admin 未启动）是否启动失败。
     * <p>
     * 默认为 true：fail-fast 才能守住“声明了隔离但模块缺失就静默串数据”的底线。
     * 只有元数据经网络获取的应用（如 workflow 的 remote 源）才建议在 dev 置 false，
     * 此时跳过校验并记 error——注意首个路由请求仍会因取不到元数据而失败，
     * 只是把失败从启动期推迟到了请求期。
     */
    private boolean failOnUnavailable = true;
}
