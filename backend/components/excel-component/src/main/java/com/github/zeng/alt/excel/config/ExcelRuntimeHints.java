package com.github.zeng.alt.excel.config;

import com.github.zeng.alt.excel.dynamic.AbstractDynamicColumn;
import com.github.zeng.alt.excel.dynamic.DynamicCell;
import com.github.zeng.alt.excel.fesod.handler.I18nHeadWriteHandler;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.ResourceHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.lang.Nullable;

import java.util.List;

/**
 * 组件自身固定的 native image 可达性注册。
 * <p>
 * 业务实体（行类型）不在这里——它们是下游模块的类，由
 * {@link ExcelModelAotProcessor} 在构建期从 {@code @ExcelImport} /
 * {@code @ExcelExport} 的用法反推出来。本类只管两件事：组件自己反射用到的类型，
 * 以及 POI / fesod 通过 {@code ServiceLoader} 和资源文件加载的东西。
 * <p>
 * 注意有一类问题 hints 解决不了：fesod 的 engine 绑定路径用 cglib
 * {@code BeanMap.Generator} 在运行期生成字节码，native image 不支持这种做法。
 * 因此 native 下绑定方式必须落到 {@link ExcelBindingMode#REFLECTIVE}
 * （默认的 {@code AUTO} 会自动切）。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public class ExcelRuntimeHints implements RuntimeHintsRegistrar {

    /**
     * POI 通过 {@code ServiceLoader} 找工作簿实现，工厂类必须可反射实例化。
     * <p>
     * 用类名字符串而不是 {@code Class} 引用：本组件不直接依赖 poi-ooxml 的这些类，
     * 写成硬引用会在缺少该依赖时编译不过。
     */
    private static final List<String> POI_FACTORIES = List.of(
            "org.apache.poi.xssf.usermodel.XSSFWorkbookFactory",
            "org.apache.poi.hssf.usermodel.HSSFWorkbookFactory");

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        registerComponentTypes(hints.reflection());
        registerPoiFactories(hints.reflection(), classLoader);
        registerResources(hints.resources());
    }

    private static void registerComponentTypes(ReflectionHints reflection) {
        // 动态列：单元格由本组件反射实例化并 set 值
        reflection.registerType(DynamicCell.class,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS,
                MemberCategory.DECLARED_FIELDS);
        // 动态列基类的字段要可见，子类的 accessor 才能跳过它
        reflection.registerType(AbstractDynamicColumn.class, MemberCategory.DECLARED_FIELDS);
        reflection.registerType(I18nHeadWriteHandler.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
    }

    private static void registerPoiFactories(ReflectionHints reflection, @Nullable ClassLoader classLoader) {
        for (String factory : POI_FACTORIES) {
            if (isPresent(factory, classLoader)) {
                reflection.registerType(TypeReference.of(factory), MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
            }
        }
    }

    private static void registerResources(ResourceHints resources) {
        // i18n 消息文件及其各语言变体
        resources.registerPattern("excel.properties");
        resources.registerPattern("excel_*.properties");
        // POI / fesod 的 ServiceLoader 描述文件与内建资源
        resources.registerPattern("META-INF/services/org.apache.poi.ss.usermodel.WorkbookProvider");
        resources.registerPattern("org/apache/poi/schemas/*");
    }

    private static boolean isPresent(String className, @Nullable ClassLoader classLoader) {
        try {
            Class.forName(className, false, classLoader);
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}
