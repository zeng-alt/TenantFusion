package com.github.zeng.alt.excel.poi;

import com.github.zeng.alt.excel.config.ExcelProperties;
import com.github.zeng.alt.excel.exception.ExcelWriteException;
import com.github.zeng.alt.excel.read.ExcelErrorReport;
import com.github.zeng.alt.excel.write.ExcelErrorWriteSpec;
import io.vavr.control.Try;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.util.Assert;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 基于 POI 编辑工作簿副本，保留原始值、公式、样式与工作表。
 * 此操作需要在内存中打开原工作簿，适合受上传大小限制的错误反馈文件。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class PoiExcelErrorWriteSpec implements ExcelErrorWriteSpec {

    private final ExcelErrorReport report;
    private Object source;
    private Object target;
    private int sheetIndex;
    private String sheetName;
    private String password;
    private String summarySheet;
    private boolean highlight;
    private boolean comments;
    private boolean autoCloseStream = true;

    /** 创建单次错误文件写出链。 */
    public PoiExcelErrorWriteSpec(ExcelErrorReport report, ExcelProperties.ErrorReport properties) {
        this.report = report;
        this.summarySheet = properties.getSheetName();
        this.highlight = properties.isHighlight();
        this.comments = properties.isComments();
    }

    @Override
    public ExcelErrorWriteSpec from(InputStream source) {
        this.source = source;
        return this;
    }

    @Override
    public ExcelErrorWriteSpec from(Path source) {
        this.source = source;
        return this;
    }

    @Override
    public ExcelErrorWriteSpec to(OutputStream target) {
        this.target = target;
        return this;
    }

    @Override
    public ExcelErrorWriteSpec to(Path target) {
        this.target = target;
        return this;
    }

    @Override
    public ExcelErrorWriteSpec sheet(int index) {
        this.sheetIndex = index;
        this.sheetName = null;
        return this;
    }

    @Override
    public ExcelErrorWriteSpec sheet(String name) {
        this.sheetName = name;
        return this;
    }

    @Override
    public ExcelErrorWriteSpec password(String password) {
        this.password = password;
        return this;
    }

    @Override
    public ExcelErrorWriteSpec summarySheet(String name) {
        this.summarySheet = name;
        return this;
    }

    @Override
    public ExcelErrorWriteSpec highlight(boolean enabled) {
        this.highlight = enabled;
        return this;
    }

    @Override
    public ExcelErrorWriteSpec comments(boolean enabled) {
        this.comments = enabled;
        return this;
    }

    @Override
    public ExcelErrorWriteSpec autoCloseStream(boolean enabled) {
        this.autoCloseStream = enabled;
        return this;
    }

    @Override
    public Try<Long> execute() {
        return Try.run(this::requireConfig).flatMap(ignored -> Try.withResources(this::openWorkbook).of(workbook -> {
            final Sheet sheet = sheetName == null ? workbook.getSheetAt(sheetIndex) : workbook.getSheet(sheetName);
            Assert.notNull(sheet, "指定的错误工作表不存在");
            new ExcelErrorSheetHelper(workbook, highlight, comments).annotate(sheet, report, summarySheet);
            write(workbook);
            return (long) report.errors().size();
        })).recoverWith(cause -> Try.failure(new ExcelWriteException("错误 Excel 生成失败", cause)));
    }

    private void requireConfig() throws Exception {
        Assert.notNull(source, "请指定原文件");
        Assert.notNull(target, "请指定输出目标");
        Assert.hasText(summarySheet, "错误明细表名称不能为空");
        if (source instanceof Path input && target instanceof Path output
                && (input.toAbsolutePath().normalize().equals(output.toAbsolutePath().normalize())
                || Files.exists(output) && Files.isSameFile(input, output))) {
            throw new ExcelWriteException("错误文件不能覆盖原文件");
        }
    }

    private Workbook openWorkbook() throws Exception {
        try (InputStream input = source instanceof Path path ? Files.newInputStream(path) : (InputStream) source) {
            return WorkbookFactory.create(input, password);
        }
    }

    private void write(Workbook workbook) throws Exception {
        if (target instanceof OutputStream output && !autoCloseStream) {
            workbook.write(output);
            output.flush();
            return;
        }
        try (OutputStream output = target instanceof Path path ? Files.newOutputStream(path) : (OutputStream) target) {
            workbook.write(output);
        }
    }
}
