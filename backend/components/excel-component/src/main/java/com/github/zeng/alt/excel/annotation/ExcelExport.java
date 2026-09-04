package com.github.zeng.alt.excel.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 把 controller 方法的返回值直接写成 Excel 附件下发。
 * <p>
 * 方法返回 {@code Collection<T>} 或 {@code Flowable<T>}，元素类型即导出实体；
 * 组件负责设置 {@code Content-Disposition}、文件名编码与流关闭，方法体里不需要碰
 * {@code HttpServletResponse}。
 * <p>
 * 用法：
 * <pre>{@code
 * @GetMapping("/export")
 * @ExcelExport(fileName = "用户清单", sheetName = "用户")
 * public List<UserVO> exportUsers(UserQry qry) {
 *     return userService.list(qry);
 * }
 * }</pre>
 * 返回 {@code Flowable} 时按 {@code alt.excel.write.batch-size} 分批写出，
 * 内存占用与数据量无关。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcelExport {

    /**
     * 下载文件名（不含扩展名），留空则用方法名。支持 {@code {i18n.key}} 形式。
     *
     * @return 文件名
     */
    String value() default "";

    /**
     * 下载文件名，与 {@link #value()} 等价，二者都给时以本项为准。
     *
     * @return 文件名
     */
    String fileName() default "";

    /**
     * 工作表名，留空则由 fesod 生成。支持 {@code {i18n.key}} 形式。
     *
     * @return 工作表名
     */
    String sheetName() default "";

    /**
     * 导出实体类型。
     * <p>
     * 默认 {@code Object.class} 表示从方法返回值的泛型实参推断；返回值是原始类型
     * 或泛型被擦除时必须显式指定。
     *
     * @return 实体类型
     */
    Class<?> type() default Object.class;

    /**
     * 是否给文件名追加时间戳，避免浏览器缓存与重名覆盖。
     *
     * @return true 表示追加
     */
    boolean timestamp() default true;

    /**
     * 列宽自适应，{@code -1} 表示取配置项 {@code alt.excel.write.auto-width}。
     *
     * @return 0 关闭、1 开启、-1 用默认
     */
    int autoWidth() default -1;

    /**
     * 表头国际化，{@code -1} 表示取配置项 {@code alt.excel.write.i18n-head}。
     *
     * @return 0 关闭、1 开启、-1 用默认
     */
    int i18nHead() default -1;
}
