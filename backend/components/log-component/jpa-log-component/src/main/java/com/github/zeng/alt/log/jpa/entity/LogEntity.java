package com.github.zeng.alt.log.jpa.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志实体。
 * <p>
 * 映射 {@code sys_oper_log} 表，与 {@link com.github.zeng.alt.log.OperLogEvent} 对应。
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
@Data
@Entity
@Table(name = "sys_oper_log")
public class LogEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long operId;

    /** 租户ID */
    private String tenantId;

    /** 操作模块 */
    private String title;

    /** 业务类型（0其它 1新增 2修改 3删除） */
    private Integer businessType;

    /** 请求方法 */
    private String method;

    /** 请求方式 */
    private String requestMethod;

    /** 操作类别（0其它 1后台用户 2手机端用户） */
    private Integer operatorType;

    /** 操作人员 */
    private String operName;

    /** 部门名称 */
    private String deptName;

    /** 请求URL */
    private String operUrl;

    /** 操作地址 */
    @Column(length = 128)
    private String operIp;

    /** 操作地点 */
    private String operLocation;

    /** 请求参数 */
    @Column(columnDefinition = "TEXT")
    private String operParam;

    /** 返回参数 */
    @Column(columnDefinition = "TEXT")
    private String jsonResult;

    /** 操作状态（0正常 1异常） */
    private Integer status;

    /** 错误消息 */
    @Column(columnDefinition = "TEXT")
    private String errorMsg;

    /** 操作时间 */
    private LocalDateTime operTime;

    /** 消耗时间（毫秒） */
    private Long costTime;
}
