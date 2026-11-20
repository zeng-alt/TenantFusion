package com.github.zeng.alt.security.api;

import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.util.Assert;

import java.util.Locale;

/**
 * 按请求方法和应用内路径匹配的白名单规则。
 *
 * @param method HTTP 方法；空字符串表示所有方法，配置绑定时允许省略
 * @param path 以斜杠开头的 PathPattern 路径
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record WhiteListRule(
    String method,
    String path
) {
    @ConstructorBinding
    public WhiteListRule {
        method = method == null ? "" : method.toUpperCase(Locale.ROOT);
        Assert.hasText(path, "白名单路径不能为空");
        Assert.isTrue(path.startsWith("/"), "白名单路径必须以 / 开头");
    }

    public WhiteListRule(String path) {
        this("", path);
    }
}
