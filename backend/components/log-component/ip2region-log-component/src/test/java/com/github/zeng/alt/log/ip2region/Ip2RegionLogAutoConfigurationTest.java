package com.github.zeng.alt.log.ip2region;

import com.github.zeng.alt.json.JsonConfiguration;
import com.github.zeng.alt.log.core.config.LogAutoConfiguration;
import com.github.zeng.alt.log.core.support.IpLocationResolver;
import io.vavr.control.Option;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.junit.jupiter.api.io.TempDir;
import org.lionsoul.ip2region.xdb.Searcher;
import org.lionsoul.ip2region.xdb.Util;
import org.lionsoul.ip2region.xdb.Version;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 通过真实 xdb 查询器验证双协议定位和可选装配，不依赖外部数据库下载。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class Ip2RegionLogAutoConfigurationTest {
    @TempDir
    Path directory;

    @Test
    void resolvesIpv4AndIpv6FromRealXdbData() throws Exception {
        final Path ipv4 = database("v4.xdb", Version.IPv4, "113.92.157.29");
        final Path ipv6 = database("v6.xdb", Version.IPv6, "240e:3b7:3272:d8d0:db09:c067:8d59:5300");
        runner().withPropertyValues(
                "alt.log.ip2region.ipv4-database=" + ipv4.toUri(),
                "alt.log.ip2region.ipv6-database=" + ipv6.toUri()
        ).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(IpLocationResolver.class);
            final var resolver = context.getBean(IpLocationResolver.class);
            assertThat(resolver.resolve("113.92.157.29")).contains("中国 广东省 深圳市 电信");
            assertThat(resolver.resolve("240e:3b7:3272:d8d0:db09:c067:8d59:5301"))
                    .contains("中国 广东省 深圳市 电信");
            assertThat(resolver.resolve("8.8.8.8")).isEmpty();
            assertThat(resolver.resolve("not-an-ip")).isEmpty();
            assertThat(resolver.resolve("")).isEmpty();
            assertThat(resolver.resolve(null)).isEmpty();
        });
    }

    @Test
    void keepsWorkingWhenDatabaseIsUnconfiguredMissingOrCorrupt() throws Exception {
        runner().run(context -> assertThat(context.getBean(IpLocationResolver.class).resolve("8.8.8.8")).isEmpty());
        runner().withPropertyValues("alt.log.ip2region.ipv4-database=file:/missing/ip2region.xdb")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(IpLocationResolver.class).resolve("8.8.8.8")).isEmpty();
                });
        final Path corrupt = Files.write(directory.resolve("corrupt.xdb"), new byte[]{1, 2, 3});
        runner().withPropertyValues("alt.log.ip2region.ipv4-database=" + corrupt.toUri())
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void customResolverTakesPrecedenceAndCanDisableTheAdapter() {
        final IpLocationResolver custom = ip -> Option.some("custom location");
        runner().withBean(IpLocationResolver.class, () -> custom).run(context ->
                assertThat(context.getBean(IpLocationResolver.class)).isSameAs(custom));
        runner().withPropertyValues("alt.log.ip2region.enabled=false").run(context -> {
            assertThat(context).hasNotFailed().doesNotHaveBean(Ip2RegionIpLocationResolver.class);
            assertThat(context.getBean(IpLocationResolver.class).resolve("8.8.8.8")).isEmpty();
        });
    }

    @Test
    void registersStandardClasspathDatabasesForNativeImages() {
        final RuntimeHints hints = new RuntimeHints();
        new Ip2RegionRuntimeHints().registerHints(hints, getClass().getClassLoader());
        assertThat(RuntimeHintsPredicates.resource().forResource("ip2region/ip2region_v4.xdb").test(hints)).isTrue();
    }

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                JsonConfiguration.class,
                JacksonAutoConfiguration.class,
                Ip2RegionLogAutoConfiguration.class,
                LogAutoConfiguration.class
        ));
    }

    private Path database(String filename, Version version, String address) throws Exception {
        // A single /24 or /120 fixture segment using the public xdb v3 layout.
        final byte[] region = "中国|广东省|深圳市|电信|CN".getBytes(StandardCharsets.UTF_8);
        final int regionPointer = Searcher.XdbMinBytes;
        final int indexPointer = regionPointer + region.length;
        final ByteBuffer buffer = ByteBuffer.allocate(indexPointer + version.segmentIndexSize)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort(0, (short) Searcher.STRUCTURE_30);
        buffer.putShort(2, (short) 1);
        buffer.putInt(8, indexPointer);
        buffer.putInt(12, indexPointer);
        buffer.putShort(16, (short) version.id);
        buffer.putShort(18, (short) 4);
        final byte[] start = Util.parseIP(address);
        final int vector = Searcher.HeaderInfoLength + ((start[0] & 255) * 256 + (start[1] & 255)) * 8;
        buffer.putInt(vector, indexPointer);
        buffer.putInt(vector + 4, indexPointer);
        start[start.length - 1] = 0;
        final byte[] end = start.clone();
        end[end.length - 1] = (byte) 255;
        version.putBytes(buffer.array(), indexPointer, start);
        version.putBytes(buffer.array(), indexPointer + version.bytes, end);
        buffer.putShort(indexPointer + version.bytes * 2, (short) region.length);
        buffer.putInt(indexPointer + version.bytes * 2 + 2, regionPointer);
        buffer.position(regionPointer).put(region);
        return Files.write(directory.resolve(filename), buffer.array());
    }
}
