package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.security.abac.method.AbacSchemaCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 将宿主主体字段贡献到授权上下文目录。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Configuration(proxyBeanMethods = false)
public class AbacSchemaConfiguration {
    @Bean
    public AbacSchemaCustomizer adminAbacSchemaCustomizer() {
        return schema -> schema.field("principal", "id", Long.class).field("principal", "deptId", Long.class);
    }
}
