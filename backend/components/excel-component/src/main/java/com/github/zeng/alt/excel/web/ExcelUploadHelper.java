package com.github.zeng.alt.excel.web;

import io.reactivex.rxjava3.core.Flowable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.function.Function;

/**
 * 上传文件落盘与清理，内部便利类。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
final class ExcelUploadHelper {

    private static final Log LOG = LogFactory.getLog(ExcelUploadHelper.class);
    private static final String TEMP_PREFIX = "excel-upload-";
    private static final String TEMP_SUFFIX = ".xlsx";

    private ExcelUploadHelper() {
    }

    /**
     * 把上传内容落到临时文件，再交给 {@code streamFactory} 建流，流终结时删除临时文件。
     * <p>
     * 必须落盘的原因：{@code Flowable} 是懒执行的，而 multipart 的原始存储在请求
     * 结束时就被 servlet 容器回收了，订阅时再去读原始流必然失败。
     *
     * @param file          上传文件
     * @param tempDir       临时目录，空则用系统临时目录
     * @param streamFactory 由临时文件建流
     * @param <T>           行类型
     * @return 行流；订阅时才真正落盘与解析
     */
    static <T> Flowable<T> spilledStream(MultipartFile file, String tempDir,
                                         Function<File, Flowable<T>> streamFactory) {
        return Flowable.using(
                () -> spill(file, tempDir),
                path -> streamFactory.apply(path.toFile()),
                ExcelUploadHelper::deleteQuietly);
    }

    private static Path spill(MultipartFile file, String tempDir) throws IOException {
        Path directory = resolveDirectory(tempDir);
        Path temp = directory == null
                ? Files.createTempFile(TEMP_PREFIX, TEMP_SUFFIX)
                : Files.createTempFile(directory, TEMP_PREFIX, TEMP_SUFFIX);
        // 不用 MultipartFile#transferTo：部分容器的实现要求目标文件尚不存在，
        // 而临时文件已经被 createTempFile 建出来了
        try (InputStream source = file.getInputStream()) {
            Files.copy(source, temp, StandardCopyOption.REPLACE_EXISTING);
        }
        return temp;
    }

    private static Path resolveDirectory(String tempDir) throws IOException {
        if (!StringUtils.hasText(tempDir)) {
            return null;
        }
        Path directory = Paths.get(tempDir);
        Files.createDirectories(directory);
        return directory;
    }

    /**
     * 删除临时文件。
     * <p>
     * 只记日志不抛异常：这里是 {@code Flowable.using} 的 disposer，抛出会变成
     * RxJava 的 undeliverable error，把真正的业务异常盖掉。
     */
    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            LOG.warn("临时文件删除失败: " + path, e);
        }
    }
}
