package com.github.zeng.alt.oss.jpa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.oss.jpa.entity.OssFileEntity;
import com.github.zeng.alt.oss.jpa.testapp.OssJpaTestApplication;
import com.github.zeng.alt.oss.jpa.repository.OssFileRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * {@link com.github.zeng.alt.oss.jpa.controller.OssFileController} REST 接口测试。
 * <p>
 * 使用 MockMvc + H2 内存数据库测试 CRUD 端点。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
@SpringBootTest(classes = OssJpaTestApplication.class, properties = {
        // OssCoreAutoConfiguration 以 oss.s3.enabled 为开关且 matchIfMissing=false，
        // 不设这一条则 OssTemplate 根本不会被创建
        "oss.s3.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:oss_controller_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false",
        "oss.thumbnail.enabled=false"
})
@AutoConfigureMockMvc
class OssFileControllerTest {


    private static Path tempDir;

    @BeforeAll
    static void setUpTempDir() throws IOException {
        tempDir = Files.createTempDirectory("jpa-oss-controller-test-");
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
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OssFileRepository repository;

    private OssFileEntity createTestEntity(String fileName) {
        OssFileEntity entity = new OssFileEntity();
        entity.setFileName(fileName);
        entity.setOriginalFileName(fileName.substring(fileName.lastIndexOf('/') + 1));
        entity.setFileSize(1024L);
        entity.setContentType("text/plain");
        entity.setBucketName("test-bucket");
        entity.setStorageType("s3");
        entity.setStatus(0);
        entity.setMd5("d41d8cd98f00b204e9800998ecf8427e");
        entity.setUrl("http://example.com/" + fileName);
        return entity;
    }

    @BeforeEach
    void setUp() {
        repository.deleteAllById(
                repository.findAll().stream()
                        .map(OssFileEntity::getFileId)
                        .toList()
        );
    }

    @Test
    void list_shouldReturnPagedResult() throws Exception {
        repository.save(createTestEntity("test/file1.txt"));
        repository.save(createTestEntity("test/file2.txt"));

        mockMvc.perform(get("/oss-files")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.pageData.length()").value(2))
                .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    void list_withPagination_shouldReturnCorrectPage() throws Exception {
        for (int i = 1; i <= 5; i++) {
            repository.save(createTestEntity("test/file" + i + ".txt"));
        }

        mockMvc.perform(get("/oss-files")
                        .param("page", "1")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageData.length()").value(2))
                .andExpect(jsonPath("$.data.total").value(5))
                .andExpect(jsonPath("$.data.pageSize").value(2))
                .andExpect(jsonPath("$.data.pageNum").value(1));
    }

    @Test
    void list_withEmptyDatabase_shouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/oss-files")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageData.length()").value(0))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void detail_withExistingId_shouldReturnEntity() throws Exception {
        OssFileEntity saved = repository.save(createTestEntity("test/detail.txt"));

        mockMvc.perform(get("/oss-files/{id}", saved.getFileId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.fileName").value("test/detail.txt"))
                .andExpect(jsonPath("$.data.fileId").value(saved.getFileId()));
    }

    @Test
    void detail_withNonExistentId_shouldReturn404() throws Exception {
        mockMvc.perform(get("/oss-files/{id}", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_shouldReturnSavedEntity() throws Exception {
        OssFileEntity entity = createTestEntity("test/create.txt");
        String json = objectMapper.writeValueAsString(entity);

        mockMvc.perform(post("/oss-files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.fileName").value("test/create.txt"))
                .andExpect(jsonPath("$.data.fileId").isNumber());
    }

    @Test
    void update_withExistingId_shouldUpdateEntity() throws Exception {
        OssFileEntity saved = repository.save(createTestEntity("test/update.txt"));
        saved.setFileName("test/updated.txt");
        saved.setFileSize(2048L);
        String json = objectMapper.writeValueAsString(saved);

        mockMvc.perform(put("/oss-files/{id}", saved.getFileId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.fileName").value("test/updated.txt"))
                .andExpect(jsonPath("$.data.fileSize").value(2048));
    }

    @Test
    void update_withNonExistentId_shouldReturn404() throws Exception {
        OssFileEntity entity = createTestEntity("test/nonexistent.txt");
        entity.setFileId(99999L);
        String json = objectMapper.writeValueAsString(entity);

        mockMvc.perform(put("/oss-files/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_withExistingId_shouldDeleteEntity() throws Exception {
        OssFileEntity saved = repository.save(createTestEntity("test/delete.txt"));

        mockMvc.perform(delete("/oss-files/{id}", saved.getFileId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 确认已被删除
        assertTrue(repository.findById(saved.getFileId()).isEmpty());
    }

    @Test
    void delete_withNonExistentId_shouldReturn404() throws Exception {
        mockMvc.perform(delete("/oss-files/{id}", 99999L))
                .andExpect(status().isNotFound());
    }
}
