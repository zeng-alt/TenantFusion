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
// 前缀必须与本组件 @ConditionalOnProperty 用的 alt.i18n 一致。
// 此处原为 sys.i18n，与同一个类里的三处 @ConditionalOnProperty、
// application-dev.yml 里注释掉的示例配置、以及项目统一的 alt.* 命名空间都对不上，
// 导致 basename 等属性永远绑定不上、只能取默认值。
@ConfigurationProperties(prefix = "alt.i18n")
public class I18nProperties {

    @NotBlank
    private String mode = "file";

    private String basename = "messages";

    @NotBlank
    private String prefix = "/api/i18n-messages";
}