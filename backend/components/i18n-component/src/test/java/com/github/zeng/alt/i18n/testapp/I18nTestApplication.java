package com.github.zeng.alt.i18n.testapp;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * i18n 集成测试共用的启动类。
 * <p>
 * <b>刻意放在独立的 testapp 子包，不能放回 {@code com.github.zeng.alt.i18n}</b>：
 * {@code I18nAutoConfiguration} 标了
 * {@code @AutoConfigurationPackage(basePackageClasses = ..., I18nMessageRepository.class)}，
 * 已把 {@code ...i18n.repository} 登记为自动配置包；启动类若位于其父包
 * {@code ...i18n}，{@code @SpringBootApplication} 会把父包再登记一次，
 * Spring Data 便会沿两条路径各注册一次 {@code i18nMessageRepository}，
 * 触发 {@code BeanDefinitionOverrideException}，整个测试上下文起不来。
 * <p>
 * 本模块主代码没有需要组件扫描的 {@code @Component}，全部由自动配置注册，
 * 因此这里不必再补 {@code @ComponentScan}。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
@SpringBootApplication
public class I18nTestApplication {
}
