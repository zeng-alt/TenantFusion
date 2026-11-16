package com.github.zeng.alt.excel.write;

import io.vavr.control.Try;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * 将读取错误标记到原工作簿的副本，并追加错误明细表。原文件不会被修改。
 * 错误位置属于一次读取的单个工作表，不能传入跨文件合并后的报告。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public interface ExcelErrorWriteSpec {

    /** 设置原文件；本次执行负责关闭输入流。 */
    ExcelErrorWriteSpec from(InputStream source);

    /** 设置原文件。 */
    ExcelErrorWriteSpec from(Path source);

    /** 设置原文件。 */
    default ExcelErrorWriteSpec from(File source) {
        return from(source.toPath());
    }

    /** 设置输出流，默认执行后关闭，可通过 autoCloseStream 关闭此行为。 */
    ExcelErrorWriteSpec to(OutputStream target);

    /** 设置输出文件，不能与原文件相同。 */
    ExcelErrorWriteSpec to(Path target);

    /** 设置输出文件。 */
    default ExcelErrorWriteSpec to(File target) {
        return to(target.toPath());
    }

    /** 设置错误所在工作表，序号从 0 开始。 */
    ExcelErrorWriteSpec sheet(int index);

    /** 设置错误所在工作表名称。 */
    ExcelErrorWriteSpec sheet(String name);

    /** 设置原文件密码。 */
    ExcelErrorWriteSpec password(String password);

    /** 设置错误明细表名称；同名时追加序号，不覆盖已有工作表。 */
    ExcelErrorWriteSpec summarySheet(String name);

    /** 是否高亮错误单元格。 */
    ExcelErrorWriteSpec highlight(boolean enabled);

    /** 是否将错误原因追加为单元格批注。 */
    ExcelErrorWriteSpec comments(boolean enabled);

    /** 是否关闭调用方提供的输出流。 */
    ExcelErrorWriteSpec autoCloseStream(boolean enabled);

    /** 生成副本，返回写入的错误明细数量；IO 或工作簿错误通过 Try 返回。 */
    Try<Long> execute();
}
