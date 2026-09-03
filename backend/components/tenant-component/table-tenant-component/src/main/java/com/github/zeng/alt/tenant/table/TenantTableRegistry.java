package com.github.zeng.alt.tenant.table;

import com.github.zeng.alt.tenant.api.TenantIgnore;
import com.github.zeng.alt.tenant.api.TenantScope;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 参与表级隔离的物理表名集合。
 * <p>
 * 表名从 JPA 元模型读取而非硬编码，避免和实体映射脱节。因为元模型属于
 * {@code EntityManagerFactory}，而本类会被 Hibernate 的 {@code StatementInspector} 调用
 * （后者由 EMF 创建），必须<b>惰性</b>初始化：EMF 尚未就绪时返回空集合，
 * 此时正处于 Hibernate 自身的 schema 校验阶段，不需要也不应该改写表名。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
@CommonsLog
public class TenantTableRegistry {

    private final ObjectProvider<EntityManagerFactory> entityManagerFactory;
    /** 显式配置的表名，不依赖元模型，构造即生效 */
    private final Set<String> baseline;
    private volatile Set<String> resolved;

    public TenantTableRegistry(
            ObjectProvider<EntityManagerFactory> entityManagerFactory,
            Set<String> extraTables) {
        this.entityManagerFactory = entityManagerFactory;
        this.baseline = extraTables == null
                ? Set.of()
                : extraTables.stream().map(String::toLowerCase)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /**
     * 取需要追加租户后缀的表名（小写）。
     *
     * @return 表名集合；元模型未就绪时只含显式配置的部分
     */
    public Set<String> tableNames() {
        Set<String> current = resolved;
        if (current != null) {
            return current;
        }
        EntityManagerFactory emf = entityManagerFactory.getIfAvailable();
        if (emf == null) {
            // 尚处于 Hibernate 自身的 schema 校验阶段，元模型还拿不到；
            // 显式配置的表名仍然生效，且不缓存，待元模型就绪后再合并
            return baseline;
        }
        synchronized (this) {
            if (resolved == null) {
                resolved = scan(emf);
                log.info("表级隔离生效表：" + resolved);
            }
            return resolved;
        }
    }

    private Set<String> scan(EntityManagerFactory emf) {
        Set<String> names = new LinkedHashSet<>(baseline);
        for (EntityType<?> entityType : emf.getMetamodel().getEntities()) {
            Class<?> javaType = entityType.getJavaType();
            if (javaType == null || javaType.isAnnotationPresent(TenantIgnore.class)) {
                continue;
            }
            TenantScope scope = javaType.getAnnotation(TenantScope.class);
            // 表级隔离只对显式标注的实体开启：SQL 文本重写有误伤风险，不做全局默认
            if (scope == null || !scope.table()) {
                continue;
            }
            Table table = javaType.getAnnotation(Table.class);
            String name = table != null && !table.name().isBlank()
                    ? table.name()
                    : entityType.getName();
            names.add(name.toLowerCase());
        }
        return Collections.unmodifiableSet(names);
    }
}
