package com.github.zeng.alt.log;

import com.github.zeng.alt.log.core.support.DefaultUserAgentResolver;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import ua_parser.Parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 桌面与移动客户端的 User-Agent 识别和未知值降级契约。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class DefaultUserAgentResolverTest {
    private static final DefaultUserAgentResolver RESOLVER = new DefaultUserAgentResolver(new Parser());

    @ParameterizedTest
    @MethodSource("clients")
    void identifiesBrowsersAndOperatingSystems(String userAgent, String browser, String os) {
        final var client = RESOLVER.resolve(userAgent);
        assertThat(client.browser()).isEqualTo(browser);
        assertThat(client.os()).isEqualTo(os);
    }

    static Stream<Arguments> clients() {
        final String windowsChrome = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
        return Stream.of(
                Arguments.of(windowsChrome, "Chrome 131.0.0", "Windows 10"),
                Arguments.of(windowsChrome + " Edg/131.0.0.0", "Edge 131.0.0", "Windows 10"),
                Arguments.of("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:133.0) "
                        + "Gecko/20100101 Firefox/133.0", "Firefox 133.0", "Ubuntu"),
                Arguments.of("Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
                        "Chrome Mobile 131.0.0", "Android 14"),
                Arguments.of("Mozilla/5.0 (iPhone; CPU iPhone OS 17_6_1 like Mac OS X) "
                        + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.6 Mobile/15E148 Safari/604.1",
                        "Mobile Safari 17.6", "iOS 17.6.1")
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "custom-unrecognized-agent"})
    void leavesUnknownClientFieldsEmpty(String userAgent) {
        final var client = RESOLVER.resolve(userAgent);
        assertThat(client.browser()).isEmpty();
        assertThat(client.os()).isEmpty();
    }

    @Test
    void handlesParserFailureWithoutBreakingLoginLogging() {
        final Parser parser = mock(Parser.class);
        when(parser.parseUserAgent("invalid")).thenThrow(new IllegalArgumentException("parse failed"));
        final var client = new DefaultUserAgentResolver(parser).resolve("invalid");
        assertThat(client.browser()).isEmpty();
        assertThat(client.os()).isEmpty();
    }
}
