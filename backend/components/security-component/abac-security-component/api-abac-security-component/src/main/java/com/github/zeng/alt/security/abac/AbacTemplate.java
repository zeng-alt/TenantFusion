package com.github.zeng.alt.security.abac;

/**
 * ABAC 门面，只负责属性约束，调用方必须先完成身份认证及基础权限检查。
 * 业务层应在写操作之前读取资源并调用 authorize(context).get()。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public interface AbacTemplate {
    AbacAuthorizationBuilder authorize(AbacContext context);
}
