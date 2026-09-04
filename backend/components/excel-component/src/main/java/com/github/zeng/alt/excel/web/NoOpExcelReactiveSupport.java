package com.github.zeng.alt.excel.web;

import com.github.zeng.alt.excel.exception.ExcelException;
import com.github.zeng.alt.excel.read.ExcelReadSpec;
import com.github.zeng.alt.excel.write.ExcelWriteSpec;
import io.vavr.control.Try;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.function.Function;

/**
 * classpath 上没有 RxJava 时装配的实现：{@link #supports(Class)} 恒为 {@code false}，
 * 其余方法给出可操作的报错而不是 {@code NoClassDefFoundError}。
 * <p>
 * 只有真的用了响应式形状的参数或返回值才会碰到它——集合形状完全不经过本类。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public class NoOpExcelReactiveSupport implements ExcelReactiveSupport {

    private static final String HINT = """
            当前 classpath 上没有 RxJava，无法使用 Flowable 形状的 Excel 导入导出。\
            请引入 io.reactivex.rxjava3:rxjava，或把参数/返回值换成 List<T> 与 \
            ExcelReadResult<T>。""";

    @Override
    public boolean supports(Class<?> type) {
        return false;
    }

    @Override
    public Object emptyStream() {
        throw new ExcelException(HINT);
    }

    @Override
    public Object streamOf(List<MultipartFile> files, String tempDir,
                           Function<File, ExcelReadSpec<?>> specFactory) {
        throw new ExcelException(HINT);
    }

    @Override
    public Try<Long> write(ExcelWriteSpec<Object> spec, Object reactiveValue) {
        return Try.failure(new ExcelException(HINT));
    }
}
