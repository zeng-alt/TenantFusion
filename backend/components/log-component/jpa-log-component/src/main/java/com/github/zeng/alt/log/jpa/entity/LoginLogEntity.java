package com.github.zeng.alt.log.jpa.entity;

import com.github.zeng.alt.domain.key.SnowflakeId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录日志实体。
 * <p>
 * 映射 {@code sys_logininfor} 表，与 {@link com.github.zeng.alt.log.LoginInfoEvent} 对应。
 *
 * @author zengJiaJun
 * @since 2026-08-12
 * @version 1.0
 */
@Getter
@Setter
@Entity
@Table(name = "sys_logininfor")
public class LoginLogEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id @SnowflakeId
    private Long id;

    /** 租户ID */
    private String tenantId;

    /** 用户账号 */
    private String username;

    /** 登录IP地址 */
    @Column(length = 128)
    private String ip;

    /** 登录状态（0成功 1失败） */
    private String status;

    /** 提示消息 */
    @Column(columnDefinition = "TEXT")
    private String message;

    /** 登录时间 */
    private LocalDateTime loginTime;
}
