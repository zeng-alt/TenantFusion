package com.github.zeng.alt.excel.write;

import io.vavr.control.Try;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;

/**
 * 模板填充链式配置面。
 * <p>
 * 模板里的占位符形如 {@code {属性}}（单值）与 {@code {.属性}}（列表）。
 * 模板文件放在启动模块的 {@code resources} 下，用 classpath 相对路径引用，
 * 例如 {@code excel/user-template.xlsx}。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public interface ExcelFillSpec {

    /**
     * 写到输出流。
     *
     * @param outputStream 输出流
     * @return this
     */
    ExcelFillSpec to(OutputStream outputStream);

    /**
     * 写到本地文件。
     *
     * @param file 文件
     * @return this
     */
    ExcelFillSpec to(File file);

    /**
     * 写到本地路径。
     *
     * @param path 路径
     * @return this
     */
    ExcelFillSpec to(Path path);

    /**
     * 工作表名。
     *
     * @param sheetName 工作表名
     * @return this
     */
    ExcelFillSpec sheet(String sheetName);

    /**
     * 写完是否关闭输出流，默认 {@code false}。
     *
     * @param autoCloseStream true 表示由本组件关闭
     * @return this
     */
    ExcelFillSpec autoCloseStream(boolean autoCloseStream);

    /**
     * 填充单个对象，对应模板里的 {@code {属性}} 占位符。
     *
     * @param data 数据对象，字段名与占位符同名
     * @return 填充的对象数（恒为 1）；失败包在 {@code Try} 里
     */
    Try<Long> fill(Object data);

    /**
     * 填充列表，对应模板里的 {@code {.属性}} 占位符。
     *
     * @param data 数据集合
     * @return 填充的对象数；失败包在 {@code Try} 里
     */
    Try<Long> fill(Collection<?> data);
}
