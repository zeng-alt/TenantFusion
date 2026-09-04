package com.github.zeng.alt.oss.jpa;

import com.github.zeng.alt.oss.OssFileInfo;
import com.github.zeng.alt.oss.jpa.entity.OssFileEntity;
import com.github.zeng.alt.oss.jpa.testapp.OssJpaTestApplication;
import com.github.zeng.alt.oss.jpa.repository.OssFileRepository;
import com.github.zeng.alt.oss.jpa.service.JpaOssFileRecordService;
import io.vavr.control.Option;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Helper methods for tests using BaseRepository.
 */
interface OssJpaRepositoryTestHelper {
    default void deleteAll(OssFileRepository repo) {
        repo.deleteAllById(repo.findAll().stream()
                .map(OssFileEntity::getFileId).toList());
    }
}

/**
 * {@link JpaOssFileRecordService} 测试。
 * <p>
 * 使用 H2 内存数据库测试文件记录服务的 CRUD 操作。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
@SpringBootTest(classes = OssJpaTestApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:oss_record_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false",
        "oss.thumbnail.enabled=false",
        "spring.main.web-application-type=none"
})
class JpaOssFileRecordServiceTest implements OssJpaRepositoryTestHelper {


    private static Path tempDir;

    @BeforeAll
    static void setUpTempDir() throws IOException {
        tempDir = Files.createTempDirectory("jpa-oss-record-test-");
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
    private JpaOssFileRecordService recordService;

    @Autowired
    private OssFileRepository repository;

    @BeforeEach
    void setUp() {
        deleteAll(repository);
    }

    @Test
    void saveRecord_shouldPersistFileInfo() {
        OssFileInfo info = createFileInfo("test/file.txt", 1024L);
        recordService.saveRecord(info, "file.txt");

        List<OssFileEntity> all = recordService.listAll();
        assertEquals(1, all.size());

        OssFileEntity entity = all.get(0);
        assertEquals("test/file.txt", entity.getFileName());
        assertEquals("file.txt", entity.getOriginalFileName());
        assertEquals(1024L, entity.getFileSize());
        assertEquals("text/plain", entity.getContentType());
        assertEquals("test-bucket", entity.getBucketName());
        assertEquals(".txt", entity.getFileSuffix());
        assertEquals("abc123", entity.getEtag());
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", entity.getMd5());
        assertEquals("http://example.com/test/file.txt", entity.getUrl());
        assertEquals("s3", entity.getStorageType());
        assertEquals(0, entity.getStatus());
    }

    @Test
    void saveRecord_withoutOriginalNameExtension_shouldHandleGracefully() {
        OssFileInfo info = createFileInfo("test/noext", 512L);
        info.setContentType(null);
        recordService.saveRecord(info, "noext");

        List<OssFileEntity> all = recordService.listAll();
        assertEquals(1, all.size());
        assertNull(all.get(0).getFileSuffix());
    }

    @Test
    void saveRecord_multipleRecords_shouldSaveAll() {
        recordService.saveRecord(createFileInfo("doc1.txt", 100L), "doc1.txt");
        recordService.saveRecord(createFileInfo("doc2.jpg", 200L), "doc2.jpg");
        recordService.saveRecord(createFileInfo("doc3.pdf", 300L), "doc3.pdf");

        assertEquals(3, recordService.listAll().size());
    }

    @Test
    void markDeletedByFileName_shouldSoftDeleteRecords() {
        recordService.saveRecord(createFileInfo("test/delete-me.txt", 100L), "delete-me.txt");
        recordService.saveRecord(createFileInfo("test/delete-me.txt", 200L), "delete-me.txt");

        recordService.markDeletedByFileName("test/delete-me.txt");

        List<OssFileEntity> deleted = repository.findByFileName("test/delete-me.txt");
        assertEquals(2, deleted.size());
        assertTrue(deleted.stream().allMatch(e -> e.getStatus() == 1));
    }

    @Test
    void markDeletedByFileName_withNonExistentFile_shouldDoNothing() {
        recordService.markDeletedByFileName("nonexistent.txt");
        assertEquals(0, recordService.listAll().size());
    }

    @Test
    void markDeleted_shouldSoftDeleteById() {
        recordService.saveRecord(createFileInfo("test/mark.txt", 100L), "mark.txt");
        OssFileEntity saved = recordService.listAll().get(0);

        recordService.markDeleted(saved.getFileId());

        Option<OssFileEntity> result = recordService.getById(saved.getFileId());
        assertTrue(result.isDefined());
        assertEquals(1, result.get().getStatus());
    }

    @Test
    void markDeleted_withNonExistentId_shouldDoNothing() {
        recordService.markDeleted(99999L);
        assertEquals(0, recordService.listAll().size());
    }

    @Test
    void cleanUp_shouldDeleteRecordsBeforeGivenTime() {
        OssFileInfo info = createFileInfo("test/old.txt", 100L);
        recordService.saveRecord(info, "old.txt");

        // 使用未来时间清理，所有记录都应在此时之前创建
        recordService.cleanUp(LocalDateTime.now().plusDays(1));

        assertEquals(0, recordService.listAll().size());
    }

    @Test
    void cleanUp_shouldNotDeleteRecentRecords() {
        recordService.saveRecord(createFileInfo("test/recent.txt", 100L), "recent.txt");

        // 使用过去时间清理，不会删除当前记录
        recordService.cleanUp(LocalDateTime.now().minusDays(1));

        assertEquals(1, recordService.listAll().size());
    }

    @Test
    void findExistingByMd5_withMatchingRecord_shouldReturnInfo() {
        OssFileInfo info = createFileInfo("test/dedup.txt", 100L);
        recordService.saveRecord(info, "dedup.txt");

        // createdBy 由 AuditorAware 设置为 "test-user"
        OssFileInfo found = recordService.findExistingByMd5("d41d8cd98f00b204e9800998ecf8427e", "test-user");
        assertNotNull(found);
        assertEquals("test/dedup.txt", found.getFileName());
        assertEquals("dedup.txt", found.getOriginalFileName());
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", found.getMd5());
    }

    @Test
    void findExistingByMd5_withDeletedRecord_shouldReturnNull() {
        OssFileInfo info = createFileInfo("test/deleted-dedup.txt", 100L);
        recordService.saveRecord(info, "deleted-dedup.txt");

        // 标记为已删除
        OssFileEntity saved = recordService.listAll().get(0);
        repository.deleteById(saved.getFileId());
        // 手动软删除 - 标记状态并保存为新记录
        OssFileEntity deletedEntity = new OssFileEntity();
        deletedEntity.setFileName("test/deleted-dedup.txt");
        deletedEntity.setOriginalFileName("deleted-dedup.txt");
        deletedEntity.setFileSize(100L);
        deletedEntity.setMd5("d41d8cd98f00b204e9800998ecf8427e");
        deletedEntity.setStatus(1);
        repository.save(deletedEntity);

        OssFileInfo found = recordService.findExistingByMd5("d41d8cd98f00b204e9800998ecf8427e", "test-user");
        assertNull(found);
    }

    @Test
    void findExistingByMd5_withDifferentUser_shouldReturnNull() {
        OssFileInfo info = createFileInfo("test/other-user.txt", 100L);
        recordService.saveRecord(info, "other-user.txt");

        // createdBy 由 AuditorAware 设置为 "test-user"，用 "other-user" 查询不到
        OssFileInfo found = recordService.findExistingByMd5("d41d8cd98f00b204e9800998ecf8427e", "other-user");
        assertNull(found);
    }

    @Test
    void findExistingByMd5_withNullMd5_shouldReturnNull() {
        assertNull(recordService.findExistingByMd5(null, "user1"));
    }

    @Test
    void findExistingByMd5_withNullUserId_shouldReturnNull() {
        assertNull(recordService.findExistingByMd5("d41d8cd98f00b204e9800998ecf8427e", null));
    }

    @Test
    void getById_withExistingRecord_shouldReturnEntity() {
        recordService.saveRecord(createFileInfo("test/get-by-id.txt", 100L), "get-by-id.txt");
        OssFileEntity saved = recordService.listAll().get(0);

        Option<OssFileEntity> found = recordService.getById(saved.getFileId());
        assertTrue(found.isDefined());
        assertEquals(saved.getFileId(), found.get().getFileId());
    }

    @Test
    void getById_withNonExistentId_shouldReturnNone() {
        Option<OssFileEntity> found = recordService.getById(99999L);
        assertTrue(found.isEmpty());
    }

    @Test
    void getByFileName_shouldReturnMatchingRecords() {
        recordService.saveRecord(createFileInfo("path/to/file.txt", 100L), "file.txt");
        recordService.saveRecord(createFileInfo("path/to/file.txt", 200L), "file.txt");

        List<OssFileEntity> results = recordService.getByFileName("path/to/file.txt");
        assertEquals(2, results.size());
    }

    @Test
    void getByFileName_withNoMatch_shouldReturnEmpty() {
        List<OssFileEntity> results = recordService.getByFileName("nonexistent.txt");
        assertTrue(results.isEmpty());
    }

    @Test
    void listAll_shouldReturnAllRecords() {
        recordService.saveRecord(createFileInfo("a.txt", 100L), "a.txt");
        recordService.saveRecord(createFileInfo("b.txt", 200L), "b.txt");
        recordService.saveRecord(createFileInfo("c.txt", 300L), "c.txt");

        List<OssFileEntity> all = recordService.listAll();
        assertEquals(3, all.size());
    }

    @Test
    void listAll_withEmptyDatabase_shouldReturnEmptyList() {
        List<OssFileEntity> all = recordService.listAll();
        assertTrue(all.isEmpty());
    }

    @Test
    void deleteById_shouldPhysicallyDeleteRecord() {
        recordService.saveRecord(createFileInfo("test/delete-physical.txt", 100L), "delete-physical.txt");
        OssFileEntity saved = recordService.listAll().get(0);

        recordService.deleteById(saved.getFileId());

        assertTrue(recordService.listAll().isEmpty());
    }

    @Test
    void getRepository_shouldReturnUnderlyingRepository() {
        assertNotNull(recordService.getRepository());
        assertSame(repository, recordService.getRepository());
    }

    // ==================== 辅助方法 ====================

    private OssFileInfo createFileInfo(String fileName, long size) {
        OssFileInfo info = new OssFileInfo();
        info.setFileName(fileName);
        info.setUrl("http://example.com/" + fileName);
        info.setSize(size);
        info.setContentType("text/plain");
        info.setBucketName("test-bucket");
        info.setEtag("abc123");
        info.setMd5("d41d8cd98f00b204e9800998ecf8427e");
        return info;
    }
}
