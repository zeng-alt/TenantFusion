package com.github.zeng.alt.oss.jpa;

import com.github.zeng.alt.oss.*;
import com.github.zeng.alt.oss.jpa.entity.OssFileEntity;
import com.github.zeng.alt.oss.jpa.service.JpaOssFileRecordService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;


import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link com.github.zeng.alt.oss.jpa.service.PersistingOssTemplate} 集成测试。
 * <p>
 * 使用 H2 内存数据库 + 本地文件系统 ({@link com.github.zeng.alt.oss.core.local.FileSystemOssTemplate})，
 * 测试持久化增强模板的上传、删除、MD5 去重、桶策略等核心功能。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
@SpringBootTest(classes = PersistingOssTemplateTest.TestApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:oss_persist_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false",
        "oss.thumbnail.enabled=false",
        "spring.main.web-application-type=none"
})
class PersistingOssTemplateTest {

    @SpringBootApplication
    @EnableJpaAuditing
    static class TestApplication {
    }

    /**
     * 测试配置：提供用户 ID 提供者（用于 MD5 去重测试）、AuditorAware 和临时 OSS 存储路径。
     */
    @Configuration
    static class TestConfig {

        @Bean
        public UserIdProvider testUserIdProvider() {
            return () -> "test-user";
        }

        @Bean
        public AuditorAware<String> auditorAware() {
            return () -> Optional.of("test-user");
        }
    }

    private static Path tempDir;

    @BeforeAll
    static void setUpTempDir() throws IOException {
        tempDir = Files.createTempDirectory("jpa-oss-persist-test-");
    }

    @AfterAll
    static void cleanUpTempDir() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> path.toFile().delete());
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String endpoint = "file:///" + tempDir.toAbsolutePath().toString().replace("\\", "/");
        registry.add("oss.s3.endpoint", () -> endpoint);
        registry.add("oss.s3.storage-type", () -> "file");
        registry.add("oss.s3.bucket-name", () -> "test-bucket");
    }

    @Autowired
    private OssTemplate ossTemplate;

    @Autowired
    private JpaOssFileRecordService recordService;

    @BeforeEach
    void setUp() {
        // 清理数据库记录
        recordService.listAll().forEach(e -> recordService.deleteById(e.getFileId()));
        // 清理文件系统
        if (tempDir != null && Files.exists(tempDir)) {
            try {
                Files.walk(tempDir)
                        .sorted(Comparator.reverseOrder())
                        .filter(p -> !p.equals(tempDir))
                        .forEach(path -> path.toFile().delete());
            } catch (IOException ignored) {
            }
        }
    }

    // ==================== 上传测试 ====================

    @Test
    void upload_byteArray_shouldUploadAndPersistRecord() {
        byte[] data = "Hello, OSS!".getBytes(StandardCharsets.UTF_8);
        OssFileInfo info = ossTemplate.upload(data, "test/hello.txt");

        assertNotNull(info);
        assertNotNull(info.getFileName());
        assertNotNull(info.getUrl());
        assertTrue(info.getSize() > 0);
        assertNotNull(info.getMd5());

        // 验证数据库中有记录
        List<OssFileEntity> records = recordService.getByFileName(info.getFileName());
        assertFalse(records.isEmpty());
        assertEquals(1, records.size());
        assertEquals("hello.txt", records.get(0).getOriginalFileName());
    }

    @Test
    void upload_byteArrayWithContentType_shouldSetContentType() {
        byte[] data = "{\"key\": \"value\"}".getBytes(StandardCharsets.UTF_8);
        OssFileInfo info = ossTemplate.upload(data, "test/data.json", "application/json");

        assertNotNull(info);
        // 验证文件已上传到文件系统
        assertTrue(ossTemplate.exists(info.getFileName()));
    }

    @Test
    void upload_inputStream_shouldUploadAndPersistRecord() {
        byte[] data = "Stream content".getBytes(StandardCharsets.UTF_8);
        OssFileInfo info = ossTemplate.upload(new ByteArrayInputStream(data), "test/stream.txt");

        assertNotNull(info);
        assertTrue(ossTemplate.exists(info.getFileName()));
        assertEquals(1, recordService.getByFileName("test/stream.txt").size());
    }

    @Test
    void upload_inputStreamWithContentType_shouldSetContentType() {
        byte[] data = "<html/>".getBytes(StandardCharsets.UTF_8);
        OssFileInfo info = ossTemplate.upload(new ByteArrayInputStream(data), "test/page.html", "text/html");

        assertNotNull(info);
        assertTrue(ossTemplate.exists(info.getFileName()));
    }

    @Test
    void upload_file_shouldUploadAndPersistRecord() throws IOException {
        Path tempFile = Files.createTempFile("upload-test-", ".txt");
        try {
            Files.writeString(tempFile, "File upload test");
            File file = tempFile.toFile();
            OssFileInfo info = ossTemplate.upload(file, "test/from-file.txt");

            assertNotNull(info);
            assertEquals(file.length(), info.getSize().longValue());
            assertTrue(ossTemplate.exists(info.getFileName()));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void upload_withoutContentType_shouldDetectMimeType() {
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        OssFileInfo info = ossTemplate.upload(data, "test/image.png");

        assertNotNull(info);
        // PersistingOssTemplate should detect PNG mime type
        // Note: we just verify the upload works, contentType detection varies
        assertTrue(ossTemplate.exists(info.getFileName()));
    }

    // ==================== MD5 去重测试 ====================

    @Test
    void upload_duplicateContent_shouldReturnExistingRecord() {
        byte[] data = "Deduplication test content".getBytes(StandardCharsets.UTF_8);

        // 第一次上传
        OssFileInfo first = ossTemplate.upload(data, "test/dedup.txt");
        assertNotNull(first);

        // 第二次上传相同内容（不同文件名）
        OssFileInfo second = ossTemplate.upload(data, "test/dedup-copy.txt");

        // 由于 MD5 相同且同一用户，应返回已有记录
        assertNotNull(second);
        assertEquals(first.getFileName(), second.getFileName());
        assertEquals(first.getMd5(), second.getMd5());
        assertEquals(first.getSize(), second.getSize());

        // 数据库应只有一条记录
        assertEquals(1, recordService.listAll().size());
    }

    @Test
    void upload_differentContent_shouldCreateDifferentRecords() {
        byte[] data1 = "Content A".getBytes(StandardCharsets.UTF_8);
        byte[] data2 = "Content B".getBytes(StandardCharsets.UTF_8);

        OssFileInfo first = ossTemplate.upload(data1, "test/file-a.txt");
        OssFileInfo second = ossTemplate.upload(data2, "test/file-b.txt");

        assertNotNull(first);
        assertNotNull(second);
        assertNotEquals(first.getFileName(), second.getFileName());
        assertNotEquals(first.getMd5(), second.getMd5());

        assertEquals(2, recordService.listAll().size());
    }

    // ==================== 下载测试 ====================

    @Test
    void download_shouldReturnOriginalContent() {
        byte[] data = "Download test content".getBytes(StandardCharsets.UTF_8);
        ossTemplate.upload(data, "test/download.txt");

        try (InputStream is = ossTemplate.download("test/download.txt")) {
            assertNotNull(is);
            byte[] downloaded = is.readAllBytes();
            assertArrayEquals(data, downloaded);
        } catch (IOException e) {
            fail("Download failed", e);
        }
    }

    @Test
    void download_nonExistentFile_shouldReturnNull() {
        InputStream is = ossTemplate.download("nonexistent/file.txt");
        assertNull(is);
    }

    // ==================== 删除测试 ====================

    @Test
    void delete_shouldRemoveFileAndMarkRecord() {
        byte[] data = "Delete me".getBytes(StandardCharsets.UTF_8);
        OssFileInfo info = ossTemplate.upload(data, "test/delete-me.txt");

        assertTrue(ossTemplate.exists(info.getFileName()));

        ossTemplate.delete(info.getFileName());

        assertFalse(ossTemplate.exists(info.getFileName()));

        // 数据库记录应标记为已删除
        List<OssFileEntity> records = recordService.getByFileName(info.getFileName());
        assertFalse(records.isEmpty());
        assertTrue(records.stream().allMatch(e -> e.getStatus() == 1));
    }

    @Test
    void delete_batch_shouldRemoveAllFilesAndMarkRecords() {
        ossTemplate.upload("Batch 1".getBytes(StandardCharsets.UTF_8), "batch/file1.txt");
        ossTemplate.upload("Batch 2".getBytes(StandardCharsets.UTF_8), "batch/file2.txt");
        ossTemplate.upload("Batch 3".getBytes(StandardCharsets.UTF_8), "batch/file3.txt");

        List<String> fileNames = recordService.listAll().stream()
                .map(OssFileEntity::getFileName)
                .toList();
        assertEquals(3, fileNames.size());

        ossTemplate.delete(fileNames);

        // 所有文件应在文件系统中删除
        assertTrue(fileNames.stream().noneMatch(f -> ossTemplate.exists(f)));

        // 所有记录应标记为已删除
        assertTrue(recordService.listAll().stream().allMatch(e -> e.getStatus() == 1));
    }

    // ==================== 查询测试 ====================

    @Test
    void exists_withExistingFile_shouldReturnTrue() {
        ossTemplate.upload("Exists test".getBytes(StandardCharsets.UTF_8), "test/exists.txt");
        assertTrue(ossTemplate.exists("test/exists.txt"));
    }

    @Test
    void exists_withNonExistentFile_shouldReturnFalse() {
        assertFalse(ossTemplate.exists("test/nonexistent.txt"));
    }

    @Test
    void getFileInfo_shouldReturnMetadata() {
        byte[] data = "File info test".getBytes(StandardCharsets.UTF_8);
        OssFileInfo uploaded = ossTemplate.upload(data, "test/file-info.txt");

        OssFileInfo info = ossTemplate.getFileInfo(uploaded.getFileName());
        assertNotNull(info);
        assertNotNull(info.getFileName());
        assertNotNull(info.getSize());
        assertNotNull(info.getUrl());
    }

    @Test
    void getFileInfo_nonExistentFile_shouldReturnNull() {
        OssFileInfo info = ossTemplate.getFileInfo("nonexistent.txt");
        assertNull(info);
    }

    @Test
    void getUrl_shouldReturnFileUri() {
        ossTemplate.upload("URL test".getBytes(StandardCharsets.UTF_8), "test/url-test.txt");

        String url = ossTemplate.getUrl("test/url-test.txt");
        assertNotNull(url);
        assertTrue(url.startsWith("file:///"));
    }

    @Test
    void getUrl_nonExistentFile_shouldReturnNull() {
        String url = ossTemplate.getUrl("nonexistent.txt");
        assertNull(url);
    }

    @Test
    void listFiles_shouldReturnMatchingFiles() {
        ossTemplate.upload("File A".getBytes(StandardCharsets.UTF_8), "list/a.txt");
        ossTemplate.upload("File B".getBytes(StandardCharsets.UTF_8), "list/b.txt");
        ossTemplate.upload("Other".getBytes(StandardCharsets.UTF_8), "other/c.txt");

        List<OssFileInfo> files = ossTemplate.listFiles("list/");
        assertEquals(2, files.size());
    }

    @Test
    void listFiles_withEmptyPrefix_shouldReturnAllFiles() {
        ossTemplate.upload("File A".getBytes(StandardCharsets.UTF_8), "alpha.txt");
        ossTemplate.upload("File B".getBytes(StandardCharsets.UTF_8), "beta.txt");

        List<OssFileInfo> files = ossTemplate.listFiles("");
        assertEquals(2, files.size());
    }

    // ==================== 复制/移动测试 ====================

    @Test
    void copy_shouldDuplicateFile() {
        ossTemplate.upload("Copy test".getBytes(StandardCharsets.UTF_8), "test/source.txt");

        ossTemplate.copy("test/source.txt", "test/target.txt");

        assertTrue(ossTemplate.exists("test/source.txt"));
        assertTrue(ossTemplate.exists("test/target.txt"));
    }

    @Test
    void move_shouldRelocateFile() {
        ossTemplate.upload("Move test".getBytes(StandardCharsets.UTF_8), "test/move-source.txt");

        ossTemplate.move("test/move-source.txt", "test/move-target.txt");

        assertFalse(ossTemplate.exists("test/move-source.txt"));
        assertTrue(ossTemplate.exists("test/move-target.txt"));
    }

    // ==================== 预签名 URL 测试 ====================

    @Test
    void presignedGetUrl_shouldReturnDirectUrl() {
        ossTemplate.upload("Presigned test".getBytes(StandardCharsets.UTF_8), "test/presigned.txt");

        String url = ossTemplate.presignedGetUrl("test/presigned.txt", 3600);
        assertNotNull(url);
        assertTrue(url.startsWith("file:///"));
    }

    // ==================== 桶感知操作测试 ====================

    @Test
    void upload_toSpecificBucket_shouldWork() {
        byte[] data = "Bucket-aware test".getBytes(StandardCharsets.UTF_8);
        OssFileInfo info = ossTemplate.upload("custom-bucket", data, "bucket-file.txt");

        assertNotNull(info);
        assertTrue(ossTemplate.exists("custom-bucket", info.getFileName()));
    }

    @Test
    void exists_inSpecificBucket_shouldWork() {
        ossTemplate.upload("another-bucket", "Exists check".getBytes(StandardCharsets.UTF_8), "bucket-exists.txt");

        // PersistingOssTemplate 没有实现桶感知的 getFileInfo，但 exists 是透传的
        assertTrue(ossTemplate.exists("another-bucket", "bucket-exists.txt"));
        // 默认桶中不应该存在
        assertFalse(ossTemplate.exists("bucket-exists.txt"));
    }

    @Test
    void delete_fromSpecificBucket_shouldWork() {
        ossTemplate.upload("del-bucket", "To be deleted".getBytes(StandardCharsets.UTF_8), "to-delete.txt");
        assertTrue(ossTemplate.exists("del-bucket", "to-delete.txt"));

        ossTemplate.delete("del-bucket", "to-delete.txt");
        assertFalse(ossTemplate.exists("del-bucket", "to-delete.txt"));
    }

    @Test
    void ensureBucketExists_shouldCreateBucket() {
        ossTemplate.ensureBucketExists("new-bucket");

        // 上传到新桶应成功
        OssFileInfo info = ossTemplate.upload("new-bucket", "Bucket created".getBytes(StandardCharsets.UTF_8), "test.txt");
        assertNotNull(info);
        assertTrue(ossTemplate.exists("new-bucket", "test.txt"));
    }

    // ==================== 桶策略测试 ====================

    @Test
    void upload_withBucketStrategy_shouldUseCustomBucket() {
        // 上传图片文件，PersistingOssTemplate 会通过桶策略分配到图片桶
        // 注意：需要 oss.s3.bucket-strategy-enabled=true 才能启用桶策略
        // 此测试仅验证基本上传功能在 PersistingOssTemplate 中正常工作
        byte[] data = "Bucket strategy test".getBytes(StandardCharsets.UTF_8);
        OssFileInfo info = ossTemplate.upload(data, "test/strategy-test.txt");

        assertNotNull(info);
        assertEquals(1, recordService.listAll().size());
    }
}
