package com.github.zeng.alt.excel;

import com.github.zeng.alt.excel.web.ExcelUploadHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 上传失败时的资源回收。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class ExcelUploadHelperTest {

    @TempDir
    Path directory;

    @Test
    void failedCopyClosesInputAndDeletesPartialFile() throws Exception {
        final AtomicBoolean closed = new AtomicBoolean();
        final InputStream input = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("broken upload");
            }

            @Override
            public void close() {
                closed.set(true);
            }
        };

        assertThatThrownBy(() -> ExcelUploadHelper.spill(input, directory.toString()))
                .isInstanceOf(IOException.class);
        assertThat(closed).isTrue();
        try (Stream<Path> files = Files.list(directory)) {
            assertThat(files).isEmpty();
        }
    }
}
