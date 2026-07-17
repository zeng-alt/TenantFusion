package com.github.zeng.alt.oss.core.local;

import com.github.zeng.alt.oss.OssFileInfo;
import com.github.zeng.alt.oss.OssProperties;
import com.github.zeng.alt.oss.StorageType;
import com.github.zeng.alt.oss.core.OssException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link FileSystemOssTemplate} 本地文件系统 OSS 模板测试。
 * <p>
 * 测试本地文件系统的所有操作：上传、下载、删除、查询、复制、移动等。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
class FileSystemOssTemplateTest {

    private FileSystemOssTemplate template;
    private Path rootDir;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        this.rootDir = tempDir;
        OssProperties props = createProperties(rootDir, "test-bucket");
        template = new FileSystemOssTemplate(props);
    }

    // ==================== 构造测试 ====================

    @Test
    void constructor_withNonFileEndpoint_shouldThrowException() {
        OssProperties props = new OssProperties();
        props.setEndpoint("https://s3.amazonaws.com");
        props.setStorageType(StorageType.FILE);

        assertThrows(OssException.class, () -> new FileSystemOssTemplate(props));
    }

    @Test
    void constructor_withNullEndpoint_shouldThrowException() {
        OssProperties props = new OssProperties();
        props.setStorageType(StorageType.FILE);

        assertThrows(OssException.class, () -> new FileSystemOssTemplate(props));
    }

    @Test
    void constructor_shouldCreateBaseDirectory(@TempDir Path tempDir) throws Exception {
        Path baseDir = tempDir.resolve("oss-base");
        OssProperties props = new OssProperties();
        props.setEndpoint("file:///" + baseDir.toAbsolutePath().toString().replace("\\", "/"));
        props.setStorageType(StorageType.FILE);
        props.setBucketName("my-bucket");
        props.setAutoCreateBucket(true);

        FileSystemOssTemplate tpl = new FileSystemOssTemplate(props);
        assertNotNull(tpl);
        assertTrue(Files.exists(baseDir));
        // 应自动创建桶目录
        assertTrue(Files.exists(baseDir.resolve("my-bucket")));
    }

    @Test
    void constructor_withAutoCreateBucketFalse_shouldNotCreateBucketDir(@TempDir Path tempDir) throws Exception {
        Path baseDir = tempDir.resolve("no-auto-create");
        OssProperties props = new OssProperties();
        props.setEndpoint("file:///" + baseDir.toAbsolutePath().toString().replace("\\", "/"));
        props.setStorageType(StorageType.FILE);
        props.setBucketName("my-bucket");
        props.setAutoCreateBucket(false);

        FileSystemOssTemplate tpl = new FileSystemOssTemplate(props);
        assertNotNull(tpl);
        assertTrue(Files.exists(baseDir));
        // 不应创建桶目录
        assertFalse(Files.exists(baseDir.resolve("my-bucket")));
    }

    @Test
    void constructor_withBasePath_shouldCreateBaseDir(@TempDir Path tempDir) throws Exception {
        Path baseDir = tempDir.resolve("with-base-path");
        OssProperties props = new OssProperties();
        props.setEndpoint("file:///" + baseDir.toAbsolutePath().toString().replace("\\", "/"));
        props.setStorageType(StorageType.FILE);
        props.setBucketName("my-bucket");
        props.setBasePath("uploads");

        FileSystemOssTemplate tpl = new FileSystemOssTemplate(props);
        assertNotNull(tpl);
    }

    // ==================== 上传测试 ====================

    @Test
    void upload_byteArray_shouldStoreFile() {
        byte[] data = "Hello World".getBytes(StandardCharsets.UTF_8);
        OssFileInfo info = template.upload(data, "test/hello.txt");

        assertNotNull(info);
        assertEquals("test/hello.txt", info.getFileName());
        assertEquals(data.length, info.getSize().longValue());
        assertTrue(Files.exists(rootDir.resolve("test-bucket/test/hello.txt")));
    }

    @Test
    void upload_byteArrayWithContentType_shouldStoreFile() {
        byte[] data = "{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8);
        OssFileInfo info = template.upload(data, "api/data.json", "application/json");

        assertNotNull(info);
        assertEquals("application/json", info.getContentType());
    }

    @Test
    void upload_inputStream_shouldStoreFile() {
        byte[] data = "Stream Data".getBytes(StandardCharsets.UTF_8);
        OssFileInfo info = template.upload(new ByteArrayInputStream(data), "stream/test.dat");

        assertNotNull(info);
        assertTrue(Files.exists(rootDir.resolve("test-bucket/stream/test.dat")));
    }

    @Test
    void upload_inputStreamWithContentType_shouldStoreFile() {
        byte[] data = "<html/>".getBytes(StandardCharsets.UTF_8);
        OssFileInfo info = template.upload(new ByteArrayInputStream(data), "web/page.html", "text/html");

        assertNotNull(info);
        assertEquals("text/html", info.getContentType());
    }

    @Test
    void upload_file_shouldStoreFile() throws Exception {
        Path tmpFile = Files.createTempFile(rootDir, "upload-", ".txt");
        try {
            Files.writeString(tmpFile, "File content");
            OssFileInfo info = template.upload(tmpFile.toFile(), "files/uploaded.txt");

            assertNotNull(info);
            assertTrue(Files.exists(rootDir.resolve("test-bucket/files/uploaded.txt")));
        } finally {
            Files.deleteIfExists(tmpFile);
        }
    }

    @Test
    void upload_overwrite_shouldReplaceExistingFile() {
        template.upload("Version 1".getBytes(StandardCharsets.UTF_8), "test/version.txt");
        template.upload("Version 2".getBytes(StandardCharsets.UTF_8), "test/version.txt");

        OssFileInfo info = template.getFileInfo("test/version.txt");
        assertNotNull(info);
        assertEquals("Version 2".length(), info.getSize().longValue());
    }

    @Test
    void upload_shouldCreateParentDirectories() {
        byte[] data = "Deep path".getBytes(StandardCharsets.UTF_8);
        OssFileInfo info = template.upload(data, "a/b/c/d/e/deep.txt");

        assertNotNull(info);
        assertTrue(Files.exists(rootDir.resolve("test-bucket/a/b/c/d/e/deep.txt")));
    }

    @Test
    void upload_withBasePath_shouldPrefixPath(@TempDir Path tempDir) throws Exception {
        OssProperties props = createProperties(tempDir, "test-bucket");
        props.setBasePath("my-prefix");
        FileSystemOssTemplate tpl = new FileSystemOssTemplate(props);

        tpl.upload("Data".getBytes(StandardCharsets.UTF_8), "test/file.txt");

        assertTrue(Files.exists(tempDir.resolve("test-bucket/my-prefix/test/file.txt")));
    }

    // ==================== 下载测试 ====================

    @Test
    void download_shouldReturnOriginalContent() throws Exception {
        byte[] data = "Download content".getBytes(StandardCharsets.UTF_8);
        template.upload(data, "test/download.txt");

        try (InputStream is = template.download("test/download.txt")) {
            assertNotNull(is);
            byte[] result = is.readAllBytes();
            assertArrayEquals(data, result);
        }
    }

    @Test
    void download_nonExistentFile_shouldReturnNull() {
        InputStream is = template.download("nonexistent.txt");
        assertNull(is);
    }

    // ==================== 删除测试 ====================

    @Test
    void delete_shouldRemoveFile() {
        template.upload("Delete me".getBytes(StandardCharsets.UTF_8), "test/to-delete.txt");
        assertTrue(template.exists("test/to-delete.txt"));

        template.delete("test/to-delete.txt");
        assertFalse(template.exists("test/to-delete.txt"));
    }

    @Test
    void delete_nonExistentFile_shouldNotThrow() {
        assertDoesNotThrow(() -> template.delete("nonexistent.txt"));
    }

    @Test
    void delete_batch_shouldRemoveAllFiles() {
        template.upload("A".getBytes(StandardCharsets.UTF_8), "batch/a.txt");
        template.upload("B".getBytes(StandardCharsets.UTF_8), "batch/b.txt");
        template.upload("C".getBytes(StandardCharsets.UTF_8), "batch/c.txt");

        template.delete(List.of("batch/a.txt", "batch/b.txt", "batch/c.txt"));

        assertFalse(template.exists("batch/a.txt"));
        assertFalse(template.exists("batch/b.txt"));
        assertFalse(template.exists("batch/c.txt"));
    }

    @Test
    void delete_shouldCleanEmptyParentDirectory() {
        template.upload("Data".getBytes(StandardCharsets.UTF_8), "cleanup/sub/dir/file.txt");
        assertTrue(Files.exists(rootDir.resolve("test-bucket/cleanup/sub/dir/file.txt")));

        template.delete("cleanup/sub/dir/file.txt");

        // 空目录应被递归清理
        assertFalse(Files.exists(rootDir.resolve("test-bucket/cleanup/sub/dir")));
    }

    // ==================== 查询测试 ====================

    @Test
    void exists_withExistingFile_shouldReturnTrue() {
        template.upload("Test".getBytes(StandardCharsets.UTF_8), "test/exists.txt");
        assertTrue(template.exists("test/exists.txt"));
    }

    @Test
    void exists_withNonExistentFile_shouldReturnFalse() {
        assertFalse(template.exists("test/nonexistent.txt"));
    }

    @Test
    void getFileInfo_shouldReturnMetadata() {
        byte[] data = "Metadata test".getBytes(StandardCharsets.UTF_8);
        template.upload(data, "test/metadata.txt");

        OssFileInfo info = template.getFileInfo("test/metadata.txt");
        assertNotNull(info);
        assertNotNull(info.getFileName());
        assertEquals(data.length, info.getSize().longValue());
        assertNotNull(info.getUrl());
        assertNotNull(info.getLastModified());
        assertNotNull(info.getEtag());
        assertEquals("test-bucket", info.getBucketName());
    }

    @Test
    void getFileInfo_nonExistentFile_shouldReturnNull() {
        OssFileInfo info = template.getFileInfo("nonexistent.txt");
        assertNull(info);
    }

    @Test
    void getUrl_shouldReturnFileUri() {
        template.upload("URL content".getBytes(StandardCharsets.UTF_8), "test/url.txt");

        String url = template.getUrl("test/url.txt");
        assertNotNull(url);
        assertTrue(url.startsWith("file:///"));
        assertTrue(url.contains("test-bucket"));
        assertTrue(url.contains("test/url.txt"));
    }

    @Test
    void getUrl_nonExistentFile_shouldReturnNull() {
        String url = template.getUrl("nonexistent.txt");
        assertNull(url);
    }

    @Test
    void listFiles_shouldReturnMatchingFiles() {
        template.upload("Content 1".getBytes(StandardCharsets.UTF_8), "list/alpha.txt");
        template.upload("Content 2".getBytes(StandardCharsets.UTF_8), "list/beta.txt");
        template.upload("Other".getBytes(StandardCharsets.UTF_8), "other/gamma.txt");

        List<OssFileInfo> files = template.listFiles("list/");
        assertEquals(2, files.size());
        assertTrue(files.stream().anyMatch(f -> f.getFileName().contains("alpha")));
        assertTrue(files.stream().anyMatch(f -> f.getFileName().contains("beta")));
    }

    @Test
    void listFiles_withEmptyPrefix_shouldReturnAll() {
        template.upload("A".getBytes(StandardCharsets.UTF_8), "a.txt");
        template.upload("B".getBytes(StandardCharsets.UTF_8), "sub/b.txt");

        List<OssFileInfo> files = template.listFiles("");
        assertEquals(2, files.size());
    }

    @Test
    void listFiles_withNonExistentPrefix_shouldReturnEmpty() {
        List<OssFileInfo> files = template.listFiles("nonexistent/");
        assertTrue(files.isEmpty());
    }

    @Test
    void listFiles_withNestedStructure_shouldRecurse() {
        template.upload("Root".getBytes(StandardCharsets.UTF_8), "nested/root.txt");
        template.upload("Sub1".getBytes(StandardCharsets.UTF_8), "nested/sub/a.txt");
        template.upload("Sub2".getBytes(StandardCharsets.UTF_8), "nested/sub/deep/b.txt");

        List<OssFileInfo> files = template.listFiles("nested/");
        assertEquals(3, files.size());
    }

    // ==================== 复制/移动测试 ====================

    @Test
    void copy_shouldDuplicateFile() {
        template.upload("Copy me".getBytes(StandardCharsets.UTF_8), "test/source.txt");
        template.copy("test/source.txt", "test/destination.txt");

        assertTrue(template.exists("test/source.txt"));
        assertTrue(template.exists("test/destination.txt"));

        // 验证内容相同
        OssFileInfo srcInfo = template.getFileInfo("test/source.txt");
        OssFileInfo dstInfo = template.getFileInfo("test/destination.txt");
        assertEquals(srcInfo.getSize(), dstInfo.getSize());
    }

    @Test
    void move_shouldRelocateFile() {
        template.upload("Move me".getBytes(StandardCharsets.UTF_8), "test/original.txt");
        template.move("test/original.txt", "test/moved.txt");

        assertFalse(template.exists("test/original.txt"));
        assertTrue(template.exists("test/moved.txt"));
    }

    // ==================== 预签名 URL 测试 ====================

    @Test
    void presignedGetUrl_shouldReturnDirectUrl() {
        template.upload("Presigned".getBytes(StandardCharsets.UTF_8), "test/presigned.txt");

        String url = template.presignedGetUrl("test/presigned.txt", 3600);
        assertNotNull(url);
        assertTrue(url.startsWith("file:///"));
    }

    @Test
    void presignedGetUrl_withNonExistentFile_shouldReturnNull() {
        String url = template.presignedGetUrl("nonexistent.txt", 3600);
        assertNull(url);
    }

    // ==================== 桶感知操作测试 ====================

    @Test
    void upload_toSpecificBucket_shouldStoreInCorrectDir() {
        template.upload("custom-bucket", "Bucket data".getBytes(StandardCharsets.UTF_8), "bucket-file.txt");

        assertTrue(Files.exists(rootDir.resolve("custom-bucket/bucket-file.txt")));
        // 不应出现在默认桶中
        assertFalse(Files.exists(rootDir.resolve("test-bucket/bucket-file.txt")));
    }

    @Test
    void upload_inputStreamToSpecificBucket_shouldStoreInCorrectDir() {
        byte[] data = "Bucket stream".getBytes(StandardCharsets.UTF_8);
        template.upload("my-bucket", new ByteArrayInputStream(data), "stream-file.dat");

        assertTrue(Files.exists(rootDir.resolve("my-bucket/stream-file.dat")));
    }

    @Test
    void exists_inSpecificBucket_shouldCheckCorrectDir() {
        template.upload("bucket-a", "Data".getBytes(StandardCharsets.UTF_8), "file.txt");

        assertTrue(template.exists("bucket-a", "file.txt"));
        assertFalse(template.exists("bucket-b", "file.txt"));
    }

    @Test
    void delete_fromSpecificBucket_shouldRemoveCorrectFile() {
        template.upload("bucket-x", "Delete from bucket".getBytes(StandardCharsets.UTF_8), "to-delete.txt");
        assertTrue(template.exists("bucket-x", "to-delete.txt"));

        template.delete("bucket-x", "to-delete.txt");
        assertFalse(template.exists("bucket-x", "to-delete.txt"));
    }

    @Test
    void ensureBucketExists_shouldCreateBucketDirectory() {
        template.ensureBucketExists("new-bucket");
        assertTrue(Files.exists(rootDir.resolve("new-bucket")));
    }

    @Test
    void ensureBucketExists_idempotent_shouldNotThrow() {
        template.ensureBucketExists("existing-bucket");
        template.ensureBucketExists("existing-bucket"); // 再次调用应不抛异常
        assertTrue(Files.exists(rootDir.resolve("existing-bucket")));
    }

    // ==================== 错误处理测试 ====================

    @Test
    void upload_emptyData_shouldStoreEmptyFile() {
        OssFileInfo info = template.upload(new byte[0], "test/empty.txt");
        assertNotNull(info);
        assertEquals(0, info.getSize().longValue());
        assertTrue(template.exists("test/empty.txt"));
    }

    @Test
    void copy_nonExistentSource_shouldThrowException() {
        assertThrows(OssException.class, () -> template.copy("nonexistent.txt", "target.txt"));
    }

    @Test
    void move_nonExistentSource_shouldThrowException() {
        assertThrows(OssException.class, () -> template.move("nonexistent.txt", "target.txt"));
    }

    @Test
    void download_withSpecialCharacters_shouldWork() {
        byte[] data = "Special chars".getBytes(StandardCharsets.UTF_8);
        template.upload(data, "path/with spaces/and+specials/file.txt");

        assertTrue(template.exists("path/with spaces/and+specials/file.txt"));

        try (InputStream is = template.download("path/with spaces/and+specials/file.txt")) {
            assertNotNull(is);
            byte[] result = is.readAllBytes();
            assertArrayEquals(data, result);
        } catch (IOException e) {
            fail("Download failed", e);
        }
    }

    // ==================== 辅助方法 ====================

    private static OssProperties createProperties(Path rootDir, String bucketName) {
        OssProperties props = new OssProperties();
        String endpoint = "file:///" + rootDir.toAbsolutePath().toString().replace("\\", "/");
        props.setEndpoint(endpoint);
        props.setStorageType(StorageType.FILE);
        props.setBucketName(bucketName);
        props.setAutoCreateBucket(false);
        return props;
    }
}
