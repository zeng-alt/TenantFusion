package com.github.zeng.alt.excel.web.reactive;

import com.github.zeng.alt.excel.annotation.ExcelImport;
import com.github.zeng.alt.excel.read.ExcelReadSpec;
import com.github.zeng.alt.excel.web.ExcelStreamSource;
import com.github.zeng.alt.excel.web.ExcelUploadHelper;
import com.github.zeng.alt.excel.web.ExcelWebSpecFactory;
import org.springframework.http.codec.multipart.FilePart;

import java.nio.file.Path;

/**
 * 把一个 {@code FilePart} 包成懒打开的读取来源，供 {@code Flowable} 形状使用。
 * <p>
 * {@code FilePart#transferTo} 是 {@code Mono}，而 {@link ExcelStreamSource#open()}
 * 是同步的，所以这里 {@code block()} 等落盘完成。这不会卡事件循环——
 * {@code open()} 由 {@code RxExcel.stream} 在 {@code Schedulers.io()} 上调用，
 * 那是允许阻塞的线程。{@code Flux} 形状不走本类，它在解析器里全程非阻塞。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
class FilePartStreamSource implements ExcelStreamSource {

    private final FilePart part;
    private final String tempDir;
    private final Class<?> rowType;
    private final ExcelImport annotation;
    private final ExcelWebSpecFactory specFactory;
    private Path temp;

    FilePartStreamSource(FilePart part, String tempDir, Class<?> rowType,
                         ExcelImport annotation, ExcelWebSpecFactory specFactory) {
        this.part = part;
        this.tempDir = tempDir;
        this.rowType = rowType;
        this.annotation = annotation;
        this.specFactory = specFactory;
    }

    @Override
    public ExcelReadSpec<?> open() throws Exception {
        this.temp = ExcelUploadHelper.createTempFile(tempDir);
        part.transferTo(temp).block();
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
