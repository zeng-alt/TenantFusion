package com.github.zeng.alt.excel.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 上传文件落盘与清理，内部便利类。
 * <p>
 * 本类刻意不引用任何响应式类型：怎么把落盘与流的生命周期绑起来是
 * {@code RxJavaExcelReactiveSupport} 的事，那才是可选依赖该出现的地方。
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
     * 把上传内容落到临时文件。
     * <p>
     * 必须落盘的原因：响应式流是懒执行的，而 multipart 的原始存储在请求结束时就被
     * servlet 容器回收了，订阅时再去读原始流必然失败。
     *
     * @param file    上传文件
     * @param tempDir 临时目录，空则用系统临时目录
     * @return 临时文件路径，调用方负责在用完后 {@link #deleteQuietly(Path)}
     * @throws IOException 落盘失败
     */
    static Path spill(MultipartFile file, String tempDir) throws IOException {
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

    /**
     * 删除临时文件。
     * <p>
     * 只记日志不抛异常：调用方通常是流的终结回调，在那里抛出会把真正的业务异常盖掉。
     *
     * @param path 临时文件
     */
    static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            LOG.warn("临时文件删除失败: " + path, e);
        }
    }

    private static Path resolveDirectory(String tempDir) throws IOException {
        if (!StringUtils.hasText(tempDir)) {
            return null;
        }
        Path directory = Paths.get(tempDir);
        Files.createDirectories(directory);
        return directory;
    }
}
