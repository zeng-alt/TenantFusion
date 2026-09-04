package com.github.zeng.alt.excel.web;

import com.github.zeng.alt.excel.read.ExcelReadSpec;
import com.github.zeng.alt.excel.rx.RxExcel;
import com.github.zeng.alt.excel.write.ExcelWriteSpec;
import io.reactivex.rxjava3.core.Flowable;
import io.vavr.control.Try;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * {@link ExcelReactiveSupport} 的 RxJava 实现。
 * <p>
 * 本类是整个 Web 层唯一引用 RxJava 的地方，且只在 classpath 上有
 * {@code io.reactivex.rxjava3.core.Flowable} 时才会被装配，因此没引 rxjava 的
 * 应用连加载它都不会发生。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public class RxJavaExcelReactiveSupport implements ExcelReactiveSupport {

    @Override
    public boolean supports(Class<?> type) {
        return type != null && Flowable.class.isAssignableFrom(type);
    }

    @Override
    public Object emptyStream() {
        return Flowable.empty();
    }

    @Override
    public Object streamOf(List<MultipartFile> files, String tempDir,
                           Function<File, ExcelReadSpec<?>> specFactory) {
        List<Flowable<?>> streams = new ArrayList<>(files.size());
        for (MultipartFile file : files) {
            streams.add(spilledStream(file, tempDir, specFactory));
        }
        return Flowable.concat(streams);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Try<Long> write(ExcelWriteSpec<Object> spec, Object reactiveValue) {
        return RxExcel.write(spec, (Flowable<Object>) reactiveValue);
    }

    /**
     * 单个上传文件的懒解析流：{@code Flowable.using} 保证订阅时落盘、终结时删文件。
     */
    private static Flowable<?> spilledStream(MultipartFile file, String tempDir,
                                             Function<File, ExcelReadSpec<?>> specFactory) {
        return Flowable.using(
                () -> ExcelUploadHelper.spill(file, tempDir),
                path -> RxExcel.stream(specFactory.apply(path.toFile())),
                ExcelUploadHelper::deleteQuietly);
    }
}
