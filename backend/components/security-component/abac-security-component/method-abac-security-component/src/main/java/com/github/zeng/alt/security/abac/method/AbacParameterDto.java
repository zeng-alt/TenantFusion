package com.github.zeng.alt.security.abac.method;

/**
 * 方法参数名称与位置的关联，试算时和真实调用一样绑定位置别名。
 * @param name 参数名或别名
 * @param index 参数位置
 * @param schema 参数字段树
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public record AbacParameterDto(String name, int index, AbacFieldDto schema) { }
