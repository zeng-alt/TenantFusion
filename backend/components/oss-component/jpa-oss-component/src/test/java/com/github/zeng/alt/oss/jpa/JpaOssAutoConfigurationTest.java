package com.github.zeng.alt.oss.jpa;

import com.github.zeng.alt.oss.OssTemplate;
import com.github.zeng.alt.oss.UserIdProvider;
import com.github.zeng.alt.oss.jpa.service.JpaOssFileRecordService;
import com.github.zeng.alt.oss.jpa.testapp.OssJpaTestApplication;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;


import static org.junit.jupiter.api.Assertions.*;

/**
 * JPA OSS 自动配置测试。
 * <p>
 * 验证 {@link com.github.zeng.alt.oss.jpa.config.JpaOssAutoConfiguration}
 * 正确创建所有必需的 Bean。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
@SpringBootTest(classes = OssJpaTestApplication.class, properties = {
        // OssCoreAutoConfiguration 以 oss.s3.enabled 为开关且 matchIfMissing=false，
        // 不设这一条则 OssTemplate 根本不会被创建
        "oss.s3.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:oss_auto_config_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false",
        "oss.thumbnail.enabled=false",
        "spring.main.web-application-type=none"
})
class JpaOssAutoConfigurationTest {


    private static Path tempDir;

    @BeforeAll
    static void setUpTempDir() throws IOException {
        tempDir = Files.createTempDirectory("jpa-oss-auto-config-test-");
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

    @Autowired(required = false)
    private OssTemplate ossTemplate;

    @Autowired(required = false)
    private JpaOssFileRecordService recordService;

    @Autowired(required = false)
    private UserIdProvider userIdProvider;

    @Test
    void ossTemplate_shouldBeCreated() {
        assertNotNull(ossTemplate, "OssTemplate bean should be created");
    }

    @Test
    void recordService_shouldBeCreated() {
        assertNotNull(recordService, "JpaOssFileRecordService bean should be created");
    }

    @Test
    void userIdProvider_shouldBeCreated() {
        assertNotNull(userIdProvider, "userIdProvider bean should be created");
    }

    @Test
    void userIdProvider_shouldReturnNull_whenNoSecurityComponent() {
        // 由于 api-security-component 不在类路径，userIdProvider 应返回 null
        assertNull(userIdProvider.getUserId());
    }

    @Test
    void ossTemplate_shouldBePersistingOssTemplate() {
        assertNotNull(ossTemplate);
        // 验证 OssTemplate 的实际类型为 PersistingOssTemplate
        assertEquals("com.github.zeng.alt.oss.jpa.service.PersistingOssTemplate",
                ossTemplate.getClass().getName());
    }

    @Test
    void ossTemplate_shouldSupportFileOperations() {
        assertNotNull(ossTemplate);
        byte[] data = "Auto-config test".getBytes();
        var info = ossTemplate.upload(data, "auto-config-test.txt");
        assertNotNull(info);
        assertTrue(ossTemplate.exists(info.getFileName()));
    }
}
