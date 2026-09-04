package com.github.zeng.alt.excel.web;

import jakarta.validation.constraints.NotNull;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 上传文件落盘与清理，内部便利类。
 * <p>
 * 本类刻意不引用任何响应式类型、也不引用任何 Web 栈的上传类型：怎么把落盘与流的
 * 生命周期绑起来是 {@code RxJavaExcelReactiveSupport} 的事，怎么拿到上传内容是
 * 各栈 {@code ExcelStreamSource} 实现的事。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public final class ExcelUploadHelper {
    @NotNull
    private static final Log LOG = LogFactory.getLog(ExcelUploadHelper.class);
    private static final String TEMP_PREFIX = "excel-upload-";
    private static final String TEMP_SUFFIX = ".xlsx";

    private ExcelUploadHelper() {
    }

    /**
     * 建一个空的临时文件，交给调用方自己往里写（WebFlux 的
     * {@code FilePart#transferTo(Path)} 需要这种形态）。
     *
     * @param tempDir 临时目录，空则用系统临时目录
     * @return 临时文件路径，调用方负责在用完后 {@link #deleteQuietly(Path)}
     * @throws IOException 创建失败
     */
    public static Path createTempFile(String tempDir) throws IOException {
        Path directory = resolveDirectory(tempDir);
        return directory == null
                ? Files.createTempFile(TEMP_PREFIX, TEMP_SUFFIX)
                : Files.createTempFile(directory, TEMP_PREFIX, TEMP_SUFFIX);
    }

    /**
     * 把上传内容落到临时文件。
     * <p>
     * 必须落盘的原因：响应式流是懒执行的，而上传内容的原始存储在请求结束时就被
     * 容器回收了，订阅时再去读原始流必然失败。
     * <p>
     * 用 {@code Files.copy} 而不是 {@code MultipartFile#transferTo}：部分容器的
     * 实现要求目标文件尚不存在，而临时文件已经被建出来了。
     *
     * @param content 上传内容，本方法负责关闭
     * @param tempDir 临时目录，空则用系统临时目录
     * @return 临时文件路径，调用方负责在用完后 {@link #deleteQuietly(Path)}
     * @throws IOException 落盘失败
     */
    public static Path spill(InputStream content, String tempDir) throws IOException {
        Path temp = createTempFile(tempDir);
        try (InputStream source = content) {
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
    public static void deleteQuietly(Path path) {
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
