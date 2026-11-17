package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.log.ip2region.Ip2RegionProperties;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证各环境的登录定位配置绑定到组件命名空间。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class LoginLogConfigurationTest {
    @ParameterizedTest
    @ValueSource(strings = {"dev", "prod"})
    void bindsDatabasePathsFromEnvironmentVariables(String profile) throws Exception {
        final StandardEnvironment environment = new StandardEnvironment();
        new YamlPropertySourceLoader().load(
                profile,
                new ClassPathResource("application-" + profile + ".yml")
        ).forEach(source -> environment.getPropertySources().addFirst(source));
        environment.getPropertySources().addFirst(new MapPropertySource(
                "login test paths",
                Map.of(
                        "IP2REGION_IPV4_DATABASE",
                        "file:/tmp/login-v4.xdb",
                        "IP2REGION_IPV6_DATABASE",
                        "file:/tmp/login-v6.xdb"
                )
        ));
        final Ip2RegionProperties properties = Binder.get(environment)
                .bind("alt.log.ip2region", Ip2RegionProperties.class).get();
        assertThat(properties.getIpv4Database()).isEqualTo("file:/tmp/login-v4.xdb");
        assertThat(properties.getIpv6Database()).isEqualTo("file:/tmp/login-v6.xdb");
    }
}
