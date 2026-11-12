package com.github.zeng.alt.security.data.core;


import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 数据权限声明式配置；管理员全量访问必须显式开启。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Data
@Accessors(chain = true)
@ConfigurationProperties(prefix = "security.data-permission")
public class DataPermissionProperties {
    private boolean administratorAll;
    private Map<String, Resource> resources = new LinkedHashMap<>();

    /** 注册受保护资源及字段映射，供类型化 customizer 使用。 */
    public DataPermissionProperties resource(String name, Class<?> entity, Consumer<Resource> customizer) {
        final var resource = new Resource().setEntity(entity);
        customizer.accept(resource);
        resources.put(name, resource);
        return this;
    }

    /**
     * 数据库实体字段映射，启动时由 JPA 元模型验证。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @Data
    @Accessors(chain = true)
    public static class Resource {
        private Class<?> entity;
        private String tenant;
        private String owner;
        private String department;
    }
}
