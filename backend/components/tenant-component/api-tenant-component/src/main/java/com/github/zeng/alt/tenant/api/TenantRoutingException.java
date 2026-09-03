package com.github.zeng.alt.tenant.api;

import com.github.zeng.alt.api.exception.BaseException;

/**
 * 租户路由失败。
 * <p>
 * 用于运行期无法定位租户目标（数据源键不存在、schema 缺失、声明的隔离档位缺少对应模块）等情形；
 * 启动期的配置组合错误直接抛 {@link IllegalStateException} 以阻断启动。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
public class TenantRoutingException extends BaseException {

    private static final String TITLE = "租户路由错误";

    public TenantRoutingException(String message) {
        super(TITLE, message);
    }

    /**
     * {@code BaseException} 没有同时接收 message 与 cause 的构造器，为避免丢失信息，
     * 这里把 cause 的描述并入 message。
     *
     * @param message 描述
     * @param cause   根因
     */
    public TenantRoutingException(String message, Throwable cause) {
        super(TITLE, message + "：" + cause.getMessage());
    }
}
