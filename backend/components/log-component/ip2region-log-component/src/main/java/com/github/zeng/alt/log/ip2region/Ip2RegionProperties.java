package com.github.zeng.alt.log.ip2region;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ip2region 离线数据库位置，支持 file: 和 classpath: 资源。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Getter
@Setter
@ConfigurationProperties("alt.log.ip2region")
public class Ip2RegionProperties {
    private boolean enabled = true;
    private String ipv4Database = "";
    private String ipv6Database = "";
}
