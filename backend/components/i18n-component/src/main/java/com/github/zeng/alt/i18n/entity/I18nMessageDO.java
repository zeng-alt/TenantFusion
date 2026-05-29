package com.github.zeng.alt.i18n.entity;

import com.github.zeng.alt.domain.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 国际化消息实体
 *
 * @author zengJiaJun
 * @since 2026年05月29日
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "alt_i18n_message", uniqueConstraints = {
        @UniqueConstraint(name = "uk_i18n_message_code_locale", columnNames = {"code", "locale"})
}, indexes = {
        @Index(name = "idx_i18n_message_code", columnList = "code"),
        @Index(name = "idx_i18n_message_locale", columnList = "locale")
})
public class I18nMessageDO extends BaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 消息编码（如：user.login.success）
     */
    @Column(name = "code", nullable = false, length = 255)
    private String code;

    /**
     * 区域（如：zh_CN、en_US）
     */
    @Column(name = "locale", nullable = false, length = 20)
    private String locale;

    /**
     * 消息内容
     */
    @Column(name = "message", nullable = false, length = 2000)
    private String message;

    /**
     * 所属模块（可选，用于分类管理）
     */
    @Column(name = "module", length = 100)
    private String module;
}
