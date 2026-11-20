package com.github.zeng.alt.security.api;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年11月27日 21:35
 */
@FunctionalInterface
public interface WhiteListService {

    /** 返回旧版不区分请求方法的公开路径，保留函数式接口兼容性。 */
    Set<String> getWhiteList();

    /** 返回允许匿名访问的请求规则。 */
    default Set<WhiteListRule> getPermitAll() {
        return getWhiteList().stream().map(WhiteListRule::new)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** 返回只要求登录的请求规则。 */
    default Set<WhiteListRule> getAuthenticated() {
        return Set.of();
    }
}
