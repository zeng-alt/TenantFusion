package com.github.zeng.alt.excel.web.servlet;

import com.github.zeng.alt.excel.annotation.ExcelImport;
import com.github.zeng.alt.excel.read.ExcelReadSpec;
import com.github.zeng.alt.excel.web.ExcelStreamSource;
import com.github.zeng.alt.excel.web.ExcelUploadHelper;
import com.github.zeng.alt.excel.web.ExcelWebSpecFactory;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

/**
 * 把一个 {@code MultipartFile} 包成懒打开的读取来源。
 * <p>
 * {@link #open()} 时才落盘并建读取链，{@link #close()} 时删除临时文件。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
class MultipartStreamSource implements ExcelStreamSource {

    private final MultipartFile file;
    private final String tempDir;
    private final Class<?> rowType;
    private final ExcelImport annotation;
    private final ExcelWebSpecFactory specFactory;
    private Path temp;

    MultipartStreamSource(MultipartFile file, String tempDir, Class<?> rowType,
                          ExcelImport annotation, ExcelWebSpecFactory specFactory) {
        this.file = file;
        this.tempDir = tempDir;
        this.rowType = rowType;
        this.annotation = annotation;
        this.specFactory = specFactory;
    }

    @Override
    public ExcelReadSpec<?> open() throws Exception {
        this.temp = ExcelUploadHelper.spill(file.getInputStream(), tempDir);
        return specFactory.readSpec(rowType, annotation).from(temp.toFile());
    }

    @Override
    public void close() {
        if (temp != null) {
            ExcelUploadHelper.deleteQuietly(temp);
            temp = null;
        }
    }
}
