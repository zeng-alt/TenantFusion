package com.github.zeng.alt.excel.support;

import io.vavr.control.Try;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 为整文件校验提供可重读的数据源，输入流落盘后由本对象回收。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Slf4j
@RequiredArgsConstructor
public final class ExcelReplayHelper implements AutoCloseable {

    @Getter
    private final File file;
    private final boolean temporary;

    /** 打开文件或接管输入流；已有文件不由本对象删除。 */
    public static ExcelReplayHelper open(Object source) throws IOException {
        if (source instanceof File file) {
            return new ExcelReplayHelper(file, false);
        }
        final Path temp = Try.of(() -> Files.createTempFile("excel-replay-", ".xlsx"))
                .onFailure(cause -> Try.run(((InputStream) source)::close)).get();
        final ExcelReplayHelper replay = new ExcelReplayHelper(temp.toFile(), true);
        return Try.withResources(() -> (InputStream) source)
                .of(input -> {
                    Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
                    return replay;
                }).onFailure(cause -> replay.close()).get();
    }

    @Override
    public void close() {
        if (temporary) {
            Try.run(() -> Files.deleteIfExists(file.toPath()))
                    .onFailure(cause -> log.warn("临时文件删除失败: {}", file, cause));
        }
    }
}
