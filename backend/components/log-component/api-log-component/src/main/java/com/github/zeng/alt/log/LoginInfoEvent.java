package com.github.zeng.alt.log;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录事件
 *
 * @author zengJiaJun
 * @since 2026年07月01日
 * @version 1.0
 */
@Data
public class LoginInfoEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 用户账号
     */
    private String username;

    /**
     * 登录状态 0成功 1失败
     */
    private String status;

    /**
     * 提示消息
     */
    private String message;

    /**
     * 登录IP地址
     */
    private String ip;

    /** 登录地点，可由调用方提供或在持久化时通过 IP 解析。 */
    private String loginLocation;

    /** 登录请求的 User-Agent，供日志组件解析，不直接落库。 */
    private String userAgent;

    /** 浏览器名称和版本。 */
    private String browser;

    /** 操作系统名称和版本。 */
    private String os;

    /** 登录发生时间，由源端设置，避免 MQ 消费延迟改变日志时间。 */
    private LocalDateTime loginTime;

    /**
     * 其他参数
     */
    private Object[] args;

}