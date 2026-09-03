package com.github.zeng.alt.tenant.hybrid;

import com.github.zeng.alt.tenant.api.TenantMetadata;
import com.github.zeng.alt.tenant.api.TenantMetadataProvider;
import com.github.zeng.alt.tenant.api.TenantMode;
import com.github.zeng.alt.tenant.api.TenantRouting;
import com.github.zeng.alt.tenant.core.DefaultTenantRoutingRegistry;
import com.github.zeng.alt.tenant.core.TenantProperties;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Optional;

/**
 * 混合模式路由：逐个租户决定四个隔离旋钮。
 * <p>
 * 这是整个组件里<b>唯一</b>读取租户隔离档位（{@code isolation_mode} / {@code is_row_isolated}）的地方。
 * 不引入本模块时，那些列根本不会被查询，只用单一档位的部署因此不必为按租户分发付代价。
 * <p>
 * 解析优先级：租户元数据显式给出的值 &gt; 由 {@code isolation_mode} 预设推导 &gt; 全局配置兜底。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
@CommonsLog
public class HybridTenantRoutingRegistry extends DefaultTenantRoutingRegistry {

    public HybridTenantRoutingRegistry(
            TenantProperties properties,
            ObjectProvider<TenantMetadataProvider> metadataProvider) {
        super(properties, metadataProvider);
    }

    /**
     * 把租户元数据翻译成四个独立旋钮。
     * <p>
     * 与 {@code DefaultTenantRoutingRegistry} 不同，这里不看全局 {@code mode}，
     * 而是先看元数据里是否直接给了 schema / 数据源键 / 表后缀——直接给的优先，
     * 使「独立库 + 库内非默认 schema」这类叠加组合可表达；都没给才退回预设与全局配置。
     */
    @Override
    protected TenantRouting resolveRouting(String tenantId) {
        Optional<TenantMetadata> found = findMetadata(tenantId);
        if (found.isEmpty()) {
            TenantMode fallback = globalMode();
            log.debug("租户 [" + tenantId + "] 无元数据，回落到全局档位 " + fallback);
            return TenantRouting.of(tenantId, fallback, tenantId);
        }
        TenantMetadata metadata = found.get();
        TenantMode preset = metadata.mode() == null ? globalMode() : metadata.mode();

        String dataSourceKey = metadata.dataSourceKey();
        String schemaName = metadata.schemaName();
        String tableSuffix = metadata.tableSuffix();
        boolean rowIsolated = metadata.rowIsolated() != null
                ? metadata.rowIsolated()
                : preset == TenantMode.ROW;

        // 元数据没给具体名字时，按预设补一个由租户标识推导的默认值
        if (preset == TenantMode.DATABASE && dataSourceKey == null) {
            dataSourceKey = tenantId;
        }
        if (preset == TenantMode.SCHEMA && schemaName == null) {
            schemaName = tenantId;
        }
        if (preset == TenantMode.TABLE && tableSuffix == null) {
            tableSuffix = tenantId;
        }
        return new TenantRouting(tenantId, dataSourceKey, schemaName, tableSuffix, rowIsolated);
    }
}
