package com.github.zeng.alt.log.core.support;

import com.github.zeng.alt.log.core.model.LoginClientDto;
import io.vavr.control.Try;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.util.StringUtils;
import ua_parser.OS;
import ua_parser.Parser;
import ua_parser.UserAgent;

/**
 * 使用 ua-parser 的共享规则解析浏览器、操作系统及版本。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@CommonsLog
@RequiredArgsConstructor
public final class DefaultUserAgentResolver implements UserAgentResolver {
    private static final LoginClientDto UNKNOWN = new LoginClientDto("", "");
    private final Parser parser;

    @Override
    public LoginClientDto resolve(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return UNKNOWN;
        }
        return Try.of(() -> {
            final UserAgent browser = parser.parseUserAgent(userAgent);
            final OS os = parser.parseOS(userAgent);
            return new LoginClientDto(
                    format(browser.family, browser.major, browser.minor, browser.patch),
                    format(os.family, os.major, os.minor, os.patch, os.patchMinor)
            );
        }).onFailure(error -> log.debug("Failed to parse login User-Agent", error)).getOrElse(UNKNOWN);
    }

    private String format(String family, String... versions) {
        if (!StringUtils.hasText(family) || "Other".equals(family)) {
            return "";
        }
        final String version = Arrays.stream(versions).filter(StringUtils::hasText).collect(Collectors.joining("."));
        return version.isEmpty() ? family : family + " " + version;
    }
}
