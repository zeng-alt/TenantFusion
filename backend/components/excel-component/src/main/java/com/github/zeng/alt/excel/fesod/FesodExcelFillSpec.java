package com.github.zeng.alt.excel.fesod;

import com.github.zeng.alt.excel.exception.ExcelWriteException;
import com.github.zeng.alt.excel.write.ExcelFillSpec;
import io.vavr.control.Try;
import org.apache.fesod.sheet.ExcelWriter;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.write.builder.ExcelWriterBuilder;
import org.apache.fesod.sheet.write.metadata.WriteSheet;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * {@link ExcelFillSpec} 的 fesod 实现。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public class FesodExcelFillSpec implements ExcelFillSpec {

    private final String templateLocation;
    private final FesodExcelContext context;

    private Object target;
    private String sheetName;
    private boolean autoCloseStream;

    /**
     * @param templateLocation 模板在 classpath 下的相对路径
     * @param context          共用协作对象
     */
    public FesodExcelFillSpec(String templateLocation, FesodExcelContext context) {
        this.templateLocation = templateLocation;
        this.context = context;
    }

    @Override
    public ExcelFillSpec to(OutputStream outputStream) {
        this.target = outputStream;
        return this;
    }

    @Override
    public ExcelFillSpec to(File file) {
        this.target = file;
        return this;
    }

    @Override
    public ExcelFillSpec to(Path path) {
        this.target = path == null ? null : path.toFile();
        return this;
    }

    @Override
    public ExcelFillSpec sheet(String sheetName) {
        this.sheetName = sheetName;
        return this;
    }

    @Override
    public ExcelFillSpec autoCloseStream(boolean autoCloseStream) {
        this.autoCloseStream = autoCloseStream;
        return this;
    }

    @Override
    public Try<Long> fill(Object data) {
        return data == null ? Try.success(0L) : fill(List.of(data));
    }

    @Override
    public Try<Long> fill(Collection<?> data) {
        return Try.of(() -> doFill(data == null ? List.of() : data));
    }

    private long doFill(Collection<?> data) throws Exception {
        try (InputStream template = openTemplate();
             ExcelWriter writer = createBuilder(template).build()) {
            WriteSheet sheet = sheetName == null
                    ? FesodSheet.writerSheet().build()
                    : FesodSheet.writerSheet(sheetName).build();
            data.forEach(item -> writer.fill(item, sheet));
            return data.size();
        }
    }

    private InputStream openTemplate() throws Exception {
        ClassPathResource resource = new ClassPathResource(templateLocation);
        if (!resource.exists()) {
            throw new ExcelWriteException("模板不存在: " + templateLocation);
        }
        return resource.getInputStream();
    }

    private ExcelWriterBuilder createBuilder(InputStream template) {
        ExcelWriterBuilder builder = FesodSheet.write();
        FesodParameterHelper.applyGlobal(builder, context.properties());
        context.writeCustomizers().orderedStream().forEach(customizer -> customizer.customize(builder));
        applyTarget(builder);
        builder.withTemplate(template);
        builder.autoCloseStream(autoCloseStream);
        return builder;
    }

    private void applyTarget(ExcelWriterBuilder builder) {
        if (target instanceof OutputStream outputStream) {
            builder.file(outputStream);
        } else if (target instanceof File file) {
            builder.file(file);
        } else {
            throw new ExcelWriteException("未指定输出目标，请先调用 to(...)");
        }
    }
}
