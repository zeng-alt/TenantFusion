package com.github.zeng.alt.excel.context;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * 保留请求的语言与时区，使异步表头和校验消息使用同一 Locale。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class LocaleExcelContextHandler implements ExcelContextHandler {

    @Override
    public Snapshot capture() {
        final LocaleContext captured = LocaleContextHolder.getLocaleContext();
        return () -> {
            final LocaleContext previous = LocaleContextHolder.getLocaleContext();
            LocaleContextHolder.setLocaleContext(captured);
            return () -> LocaleContextHolder.setLocaleContext(previous);
        };
    }
}
