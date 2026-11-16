package com.github.zeng.alt.excel.context;

import com.github.zeng.alt.excel.config.ExcelProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 按顺序组合上下文贡献者，逆序恢复执行线程；不安装全局 RxJava 或 Reactor hook。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public final class ExcelContextTemplate {

    private final ObjectProvider<ExcelContextHandler> handlers;
    private final ExcelProperties properties;

    /** 在调用线程捕获配置的上下文贡献者。 */
    public ExcelContextHandler.Snapshot capture() {
        return properties.getContext().isEnabled()
                ? combine(handlers.orderedStream().map(ExcelContextHandler::capture).toList())
                : ExcelContextHandler.Snapshot.empty();
    }

    /** 非 Spring 创建的读取链和静态 RxExcel 工厂使用可用的内置贡献者。 */
    public static ExcelContextHandler.Snapshot captureDefaults() {
        final List<ExcelContextHandler.Snapshot> snapshots = new ArrayList<>();
        snapshots.add(new LocaleExcelContextHandler().capture());
        final ClassLoader loader = ExcelContextTemplate.class.getClassLoader();
        if (ClassUtils.isPresent("com.github.zeng.alt.tenant.api.TenantContextHolder", loader)) {
            snapshots.add(new TenantExcelContextHandler().capture());
        }
        if (ClassUtils.isPresent("org.springframework.security.core.context.SecurityContextHolder", loader)) {
            snapshots.add(new SecurityExcelContextHandler().capture());
        }
        return combine(snapshots);
    }

    /** 组合快照，可在框架边界追加 Reactor Context 中的认证。 */
    public static ExcelContextHandler.Snapshot combine(List<ExcelContextHandler.Snapshot> snapshots) {
        final List<ExcelContextHandler.Snapshot> captured = List.copyOf(snapshots);
        return () -> {
            final List<ExcelContextHandler.Scope> scopes = new ArrayList<>();
            try {
                captured.forEach(snapshot -> scopes.add(snapshot.restore()));
            } catch (RuntimeException | Error failure) {
                scopes.reversed().forEach(ExcelContextHandler.Scope::close);
                throw failure;
            }
            return () -> scopes.reversed().forEach(ExcelContextHandler.Scope::close);
        };
    }
}
