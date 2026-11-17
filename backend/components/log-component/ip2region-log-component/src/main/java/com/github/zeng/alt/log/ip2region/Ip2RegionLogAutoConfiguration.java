package com.github.zeng.alt.log.ip2region;

import com.github.zeng.alt.log.core.config.LogAutoConfiguration;
import com.github.zeng.alt.log.core.support.IpLocationResolver;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.extern.apachecommons.CommonsLog;
import org.lionsoul.ip2region.service.Config;
import org.lionsoul.ip2region.service.ConfigBuilder;
import org.lionsoul.ip2region.service.Ip2Region;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

/**
 * 按数据库配置提供离线 IP 定位，加载失败不影响登录日志。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@CommonsLog
@AutoConfiguration(before = LogAutoConfiguration.class)
@EnableConfigurationProperties(Ip2RegionProperties.class)
@ImportRuntimeHints(Ip2RegionRuntimeHints.class)
@ConditionalOnProperty(prefix = "alt.log.ip2region", name = "enabled", havingValue = "true", matchIfMissing = true)
public class Ip2RegionLogAutoConfiguration {
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(IpLocationResolver.class)
    public Ip2RegionIpLocationResolver ipLocationResolver(Ip2RegionProperties properties, ResourceLoader resources) {
        final Option<Config> ipv4 = load(properties.getIpv4Database(), false, resources);
        final Option<Config> ipv6 = load(properties.getIpv6Database(), true, resources);
        if (ipv4.isEmpty() && ipv6.isEmpty()) {
            return new Ip2RegionIpLocationResolver(Option.none());
        }
        final Option<Ip2Region> searcher = Try.of(() -> Ip2Region.create(ipv4.getOrNull(), ipv6.getOrNull()))
                .onFailure(error -> log.warn("Failed to initialize ip2region", error)).toOption();
        return new Ip2RegionIpLocationResolver(searcher);
    }

    private Option<Config> load(String location, boolean ipv6, ResourceLoader resources) {
        return Option.of(location).filter(StringUtils::hasText).flatMap(path -> Try.withResources(
                () -> resources.getResource(path).getInputStream()
        ).of(input -> {
            final ConfigBuilder builder = Config.custom().setCachePolicy(Config.BufferCache).setXdbInputStream(input);
            return ipv6 ? builder.asV6() : builder.asV4();
        }).onFailure(error -> log.warn("Failed to load ip2region database", error)).toOption());
    }
}
