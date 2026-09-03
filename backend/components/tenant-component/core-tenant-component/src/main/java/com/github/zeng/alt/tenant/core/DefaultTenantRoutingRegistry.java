package com.github.zeng.alt.tenant.core;

import com.github.zeng.alt.tenant.api.TenantMetadata;
import com.github.zeng.alt.tenant.api.TenantMetadataProvider;
import com.github.zeng.alt.tenant.api.TenantMode;
import com.github.zeng.alt.tenant.api.TenantRouting;
import com.github.zeng.alt.tenant.api.TenantRoutingRegistry;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单档位路由：隔离档位取全局配置，只有具体的 schema 名 / 数据源键 / 表后缀按租户查元数据。
 * <p>
 * 不读租户的隔离档位字段——那是 hybrid 模块的职责。因此只用模式级的部署不会为「按租户分发」付代价。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
@CommonsLog
public class DefaultTenantRoutingRegistry implements TenantRoutingRegistry {

    private final TenantProperties properties;
    private final ObjectProvider<TenantMetadataProvider> metadataProvider;
    private final Map<String, TenantRouting> cache = new ConcurrentHashMap<>();

    public DefaultTenantRoutingRegistry(
            TenantProperties properties,
            ObjectProvider<TenantMetadataProvider> metadataProvider) {
        this.properties = properties;
        this.metadataProvider = metadataProvider;
    }

    @Override
    public TenantRouting resolve(String tenantId) {
        String effective = tenantId == null || tenantId.isBlank()
                ? properties.getDefaultTenantId()
                : tenantId;
        return cache.computeIfAbsent(effective, this::resolveRouting);
    }

    @Override
    public void evict(String tenantId) {
        if (tenantId != null) {
            cache.remove(tenantId);
        }
    }

    @Override
    public void evictAll() {
        cache.clear();
    }

    /**
     * 供子类（hybrid）复用的元数据查询。
     *
     * @param tenantId 租户标识
     * @return 元数据
     */
    protected Optional<TenantMetadata> findMetadata(String tenantId) {
        if (!Boolean.TRUE.equals(properties.getMetadata().getEnabled())) {
            return Optional.empty();
        }
        TenantMetadataProvider provider = metadataProvider.getIfAvailable();
        return provider == null ? Optional.empty() : provider.findById(tenantId);
    }

    /** 全局配置的隔离档位 */
    protected TenantMode globalMode() {
        return properties.getMode() == null ? TenantMode.NONE : properties.getMode();
    }

    protected TenantProperties properties() {
        return properties;
    }

    /**
     * 真正的解析逻辑，结果由 {@link #resolve(String)} 缓存。
     * <p>
     * 本实现只看全局档位；hybrid 模块通过覆盖本方法改为逐租户决定四个旋钮。
     *
     * @param tenantId 已归一化的租户标识，非空
     * @return 路由结果
     */
    protected TenantRouting resolveRouting(String tenantId) {
        TenantMode mode = globalMode();
        if (mode == TenantMode.NONE) {
            return TenantRouting.none(tenantId);
        }
        Optional<TenantMetadata> metadata = findMetadata(tenantId);
        // 元数据缺失时按租户标识推导默认命名，保证单档位部署零配置可用
        String name = switch (mode) {
            case SCHEMA -> metadata.map(TenantMetadata::schemaName).orElse(tenantId);
            case DATABASE -> metadata.map(TenantMetadata::dataSourceKey).orElse(tenantId);
            case TABLE -> metadata.map(TenantMetadata::tableSuffix).orElse(tenantId);
            case ROW, NONE -> null;
        };
        return TenantRouting.of(tenantId, mode, name);
    }
}
