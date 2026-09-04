package com.github.zeng.alt.excel.config;

import com.github.zeng.alt.excel.dynamic.DynamicCell;
import com.github.zeng.alt.excel.fesod.handler.I18nHeadWriteHandler;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.lang.Nullable;

/**
 * Native image 可达性注册。
 * <p>
 * 只登记本组件自己反射用到的类型与资源。业务实体（{@code @ExcelProperty} 标注的
 * DTO）由各自所在模块登记——组件不可能知道下游有哪些实体，也不该去扫全类路径。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public class ExcelRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        hints.reflection()
                .registerType(DynamicCell.class,
                        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                        MemberCategory.INVOKE_DECLARED_METHODS)
                .registerType(I18nHeadWriteHandler.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);

        // i18n 消息文件：excel.properties 及其各语言变体
        hints.resources().registerPattern("excel.properties");
        hints.resources().registerPattern("excel_*.properties");
    }
}
