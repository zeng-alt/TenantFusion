package com.github.zeng.alt.i18n.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 国际化组件配置属性
 *
 * @author zengJiaJun
 * @since 2026年05月29日
 * @version 1.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "alt.i18n")
public class I18nProperties {

    @NotBlank
    private String mode = "file";

    private String basename = "messages";

    @NotBlank
    private String prefix = "/api/i18n-messages";
}