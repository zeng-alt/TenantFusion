package com.github.zeng.alt.oss.jpa.testapp;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

/**
 * 本模块所有集成测试共用的启动类。
 * <p>
 * 原先每个测试类各自嵌套一个 {@code @SpringBootApplication}，四个类又同在
 * {@code com.github.zeng.alt.oss.jpa} 包下：任何一个启动时，组件扫描都会把另外三个
 * 当作配置类一并加载，自动配置的注册器于是重复执行，产生
 * {@code BeanDefinitionOverrideException}（ossFileRepository 等 Bean 被注册两次）。
 * <p>
 * 收敛成唯一一个启动类即可根除。各测试自己的 Bean 请用 {@code @TestConfiguration}
 * 声明——那个注解会被 {@code TypeExcludeFilter} 排除在组件扫描之外，只对所属测试生效，
 * 不会再泄漏给同包的其他测试。
 * <p>
 * <b>本类刻意放在独立的 testapp 子包，不能放回 {@code com.github.zeng.alt.oss.jpa}</b>：
 * {@code JpaOssAutoConfiguration} 标了
 * {@code @AutoConfigurationPackage(basePackageClasses = ..., OssFileRepository.class)}，
 * 已经把 {@code ...oss.jpa.repository} 登记为自动配置包；启动类若位于其父包
 * {@code ...oss.jpa}，{@code @SpringBootApplication} 会把父包也登记一次，
 * Spring Data 的仓库扫描便会沿两条路径各找到 {@code OssFileRepository} 一次，
 * 再次触发 {@code BeanDefinitionOverrideException}。
 * <p>
 * 审计不在这里标 {@code @EnableJpaAuditing}：{@code DomainAutoConfiguration} 已经开启，
 * 重复标注会重复注册 {@code jpaAuditingHandler}。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
@SpringBootApplication
// 启动类在 testapp 子包，默认只扫描该子包，扫不到 ...oss.jpa.controller 下的
// OssFileController，接口会全部 404。这里显式扫描生产包。
// 组件扫描只找 @Component，不负责注册 Spring Data 仓库（那由自动配置包决定），
// 所以不会重新引入 ossFileRepository 的重复注册。
//
// 必须显式带上这两个 excludeFilters：自定义 @ComponentScan 会整体覆盖
// @SpringBootApplication 内置的那份，缺了它们，各测试用 @TestConfiguration 声明的
// 私有 Bean 不再被排除，会被扫进所有测试的上下文里互相污染。
@ComponentScan(
        basePackages = "com.github.zeng.alt.oss.jpa",
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
                @ComponentScan.Filter(type = FilterType.CUSTOM,
                        classes = AutoConfigurationExcludeFilter.class)
        })
public class OssJpaTestApplication {

    /**
     * 审计人提供者。{@code @EnableJpaAuditing} 由 {@code DomainAutoConfiguration} 开启，
     * 但它不提供 {@code AuditorAware}，需要在此补上，否则审计字段写不进值。
     *
     * @return 固定返回测试用户
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of("test-user");
    }
}
