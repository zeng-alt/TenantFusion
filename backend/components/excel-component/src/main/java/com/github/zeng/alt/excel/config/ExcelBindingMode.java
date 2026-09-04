package com.github.zeng.alt.excel.config;

import org.springframework.core.NativeDetector;

/**
 * 实体与单元格之间的绑定方式。
 * <p>
 * 之所以需要这个开关：fesod 的实体绑定路径（读的
 * {@code ModelBuildEventListener#buildUserModel}、写的
 * {@code ExcelWriteAddExecutor#addJavaObjectToExcel}）用 cglib
 * {@code BeanMap.Generator.create()} 在运行期生成字节码。GraalVM native image
 * 不支持运行期生成字节码，这不是缺 reflection hints，注册再多 hints 也无解。
 * 而 fesod 的「无模型」路径（读返回 {@code Map<列下标, 字符串>}、写接受
 * {@code Collection} 行）不碰 cglib，本组件在它上面自建了一层反射绑定。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public enum ExcelBindingMode {

    /**
     * 按运行环境自动选：native image 里用 {@link #REFLECTIVE}，JVM 里用 {@link #ENGINE}。
     * <p>
     * 默认值。同一份代码在 JVM 上跑得快、打成 native 也能用。
     */
    AUTO,

    /**
     * 交给 fesod 自己绑定。
     * <p>
     * 更快，且 {@code @ExcelProperty(converter = ...)} 声明的自定义
     * {@code Converter}、{@code @DateTimeFormat}、{@code @NumberFormat} 全部生效。
     * <b>过不了 native image</b>（cglib 运行期生成字节码）。
     */
    ENGINE,

    /**
     * 用本组件的 {@link com.github.zeng.alt.excel.support.ExcelRowAccessor} 绑定。
     * <p>
     * 走 fesod 的无模型路径 + 普通反射，反射元数据每个类型只解析一次并缓存，
     * 配合 AOT hints 可以在 native image 下工作。代价是值转换走 Spring
     * {@code ConversionService}，不支持 fesod 的自定义 {@code Converter}。
     */
    REFLECTIVE;

    /**
     * 把 {@link #AUTO} 落实成具体模式。
     *
     * @return {@link #ENGINE} 或 {@link #REFLECTIVE}
     */
    public boolean isReflective() {
        return this == REFLECTIVE || (this == AUTO && NativeDetector.inNativeImage());
    }
}
