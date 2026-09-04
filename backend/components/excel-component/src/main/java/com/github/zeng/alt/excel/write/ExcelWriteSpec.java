package com.github.zeng.alt.excel.write;

import io.reactivex.rxjava3.core.Flowable;
import io.vavr.control.Try;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;

/**
 * 写出链式配置面。
 * <p>
 * 形状与 {@link com.github.zeng.alt.excel.read.ExcelReadSpec} 对称：一个输出目标 +
 * 若干可选项 + 一个终结步骤。终结步骤按数据来源选 {@link #write(Collection)} 或
 * {@link #write(Flowable)}。
 * <p>
 * 典型用法：
 * <pre>{@code
 * excelTemplate.write(UserVO.class)
 *         .to(response.getOutputStream())
 *         .sheet("用户")
 *         .autoWidth(true)
 *         .write(users)
 *         .getOrElseThrow(e -> new ExcelWriteException("导出失败", e));
 * }</pre>
 *
 * @param <T> 行类型
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public interface ExcelWriteSpec<T> {

    // ==================== 输出目标 ====================

    /**
     * 写到输出流。
     * <p>
     * 默认不关闭调用方的流（HTTP 响应流由容器负责），见 {@link #autoCloseStream(boolean)}。
     *
     * @param outputStream 输出流
     * @return this
     */
    ExcelWriteSpec<T> to(OutputStream outputStream);

    /**
     * 写到本地文件。
     *
     * @param file 文件
     * @return this
     */
    ExcelWriteSpec<T> to(File file);

    /**
     * 写到本地路径。
     *
     * @param path 路径
     * @return this
     */
    ExcelWriteSpec<T> to(Path path);

    // ==================== 可选项 ====================

    /**
     * 工作表名，默认由 fesod 生成。
     *
     * @param sheetName 工作表名
     * @return this
     */
    ExcelWriteSpec<T> sheet(String sheetName);

    /**
     * 列宽自适应，默认取配置项 {@code alt.excel.write.auto-width}。
     *
     * @param autoWidth true 开启
     * @return this
     */
    ExcelWriteSpec<T> autoWidth(boolean autoWidth);

    /**
     * 表头国际化，默认取配置项 {@code alt.excel.write.i18n-head}。
     * <p>
     * 开启时 {@code @ExcelProperty("{user.name}")} 会在写出时替换为当前 Locale 的文本。
     *
     * @param i18nHead true 开启
     * @return this
     */
    ExcelWriteSpec<T> i18nHead(boolean i18nHead);

    /**
     * 只导出这些字段，与 {@link #excludeColumns(Collection)} 互斥。
     *
     * @param fieldNames 实体字段名
     * @return this
     */
    ExcelWriteSpec<T> includeColumns(Collection<String> fieldNames);

    /**
     * 排除这些字段。
     *
     * @param fieldNames 实体字段名
     * @return this
     */
    ExcelWriteSpec<T> excludeColumns(Collection<String> fieldNames);

    /**
     * 给生成的文件加密码。
     *
     * @param password 密码
     * @return this
     */
    ExcelWriteSpec<T> password(String password);

    /**
     * 写完是否关闭输出流，默认 {@code false}。
     *
     * @param autoCloseStream true 表示由本组件关闭
     * @return this
     */
    ExcelWriteSpec<T> autoCloseStream(boolean autoCloseStream);

    // ==================== 终结步骤 ====================

    /**
     * 一次性写出全部数据。
     *
     * @param rows 数据行，{@code null} 或空集合会写出只含表头的文件
     * @return 写出的行数；IO 层面的失败包在 {@code Try} 里
     */
    Try<Long> write(Collection<T> rows);

    /**
     * 从响应式流写出，适合大数据量导出。
     * <p>
     * 内部按 {@code alt.excel.write.batch-size} 分批阻塞拉取——写文件本身是同步动作，
     * 这里的阻塞是链路最外层的终结步骤，不会藏在中间操作符里。
     *
     * @param rows 数据流
     * @return 写出的行数；IO 层面的失败包在 {@code Try} 里
     */
    Try<Long> write(Flowable<T> rows);
}
