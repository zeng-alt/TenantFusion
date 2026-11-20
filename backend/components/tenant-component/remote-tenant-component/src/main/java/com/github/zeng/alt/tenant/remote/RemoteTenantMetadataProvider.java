package com.github.zeng.alt.tenant.remote;

import com.github.zeng.alt.tenant.api.TenantMetadata;
import com.github.zeng.alt.tenant.api.TenantMetadataProvider;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 远程租户元数据来源：经 {@link TenantRemoteSource} 从 admin 读取，带 TTL 缓存。
 * <p>
 * 只缓存单租户查询（最高频的路由路径），{@link #findAll()} 每次直调，
 * 供启动校验与预热使用。停用/不存在的租户不缓存，避免 admin 启用租户后延迟过久。
 *
 * @author zengJiaJun
 * @since 2026年10月02日
 * @version 1.0
 */
public class RemoteTenantMetadataProvider implements TenantMetadataProvider {

    private final TenantRemoteSource remoteSource;
    private final long cacheTtlMillis;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public RemoteTenantMetadataProvider(TenantRemoteSource remoteSource, long cacheTtlMillis) {
        this.remoteSource = remoteSource;
        this.cacheTtlMillis = cacheTtlMillis;
    }

    @Override
    public Optional<TenantMetadata> findById(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return Optional.empty();
        }
        CacheEntry cached = cache.get(tenantId);
        if (cached != null && Instant.now().toEpochMilli() < cached.expiresAt) {
            return Optional.of(cached.metadata);
        }
        TenantMetadata remote = remoteSource.findById(tenantId);
        TenantMetadata metadata = toMetadata(remote);
        if (metadata == null) {
            cache.remove(tenantId);
            return Optional.empty();
        }
        cache.put(tenantId, new CacheEntry(metadata, Instant.now().toEpochMilli() + cacheTtlMillis));
        return Optional.of(metadata);
    }

    @Override
    public List<TenantMetadata> findAll() {
        return remoteSource.findAll().stream()
                .map(RemoteTenantMetadataProvider::toMetadata)
                .filter(metadata -> metadata != null)
                .toList();
    }

    @Override
    public Optional<TenantMetadata> findCurrentById(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(toMetadata(remoteSource.findById(tenantId)));
    }

    /** 主动失效单个租户缓存，供 admin 推送/定时同步调用 */
    public void evict(String tenantId) {
        if (tenantId != null) {
            cache.remove(tenantId);
        }
    }

    /** 清空全部缓存 */
    public void evictAll() {
        cache.clear();
    }

    /**
     * 远端守卫：服务端本应只返回启用租户，这里再拦一道，
     * 停用/无标识的不进缓存，避免 admin 启用租户后延迟过久。
     */
    static TenantMetadata toMetadata(TenantMetadata remote) {
        if (remote == null || remote.tenantId() == null || remote.tenantId().isBlank()) {
            return null;
        }
        if (!remote.enabled()) {
            return null;
        }
        return remote;
    }

    private record CacheEntry(TenantMetadata metadata, long expiresAt) {
    }
}
