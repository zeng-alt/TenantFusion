package com.github.zeng.alt.rest.apt.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.web.servlet.function.RouterFunction;

/**
 * rest-apt-component 自动配置
 *
 * <p>生成的 Handler (@Component) 和 Router (@Configuration) 类
 * 通过标准组件扫描自动注册，此 AutoConfiguration 作为入口标记。
 *
 * @author zengJiaJun
 * @crateTime 2026年05月28日
 * @version 1.0
 */
@AutoConfiguration
@ConditionalOnClass(RouterFunction.class)
public class RestAptAutoConfiguration {
}
