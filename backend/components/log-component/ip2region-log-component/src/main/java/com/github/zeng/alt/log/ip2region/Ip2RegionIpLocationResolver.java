package com.github.zeng.alt.log.ip2region;

import com.github.zeng.alt.log.core.support.IpLocationResolver;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.lionsoul.ip2region.service.Ip2Region;
import org.springframework.util.StringUtils;

/**
 * 通过 ip2region 的全内存查询服务解析 IPv4/IPv6 归属地。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@CommonsLog
@RequiredArgsConstructor
public final class Ip2RegionIpLocationResolver implements IpLocationResolver, AutoCloseable {
    private final Option<Ip2Region> searcher;

    @Override
    public Option<String> resolve(String ip) {
        if (!StringUtils.hasText(ip)) {
            return Option.none();
        }
        return searcher.flatMap(engine -> Try.of(() -> engine.search(ip))
                .onFailure(error -> log.debug("IP location lookup failed", error)).toOption())
                .flatMap(Option::of).filter(StringUtils::hasText)
                .map(region -> Arrays.stream(region.split("\\|", -1)).limit(4)
                        .map(String::trim).filter(part -> !part.isEmpty() && !part.equals("0"))
                        .distinct().collect(Collectors.joining(" ")))
                .filter(StringUtils::hasText);
    }

    @Override
    public void close() throws InterruptedException {
        if (searcher.isDefined()) {
            searcher.get().close();
        }
    }
}
