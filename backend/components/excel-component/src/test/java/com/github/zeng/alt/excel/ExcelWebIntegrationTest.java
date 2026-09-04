package com.github.zeng.alt.excel;

import com.github.zeng.alt.excel.annotation.ExcelExport;
import com.github.zeng.alt.excel.annotation.ExcelImport;
import com.github.zeng.alt.excel.read.ExcelReadResult;
import io.reactivex.rxjava3.core.Flowable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.github.zeng.alt.excel.config.ExcelAutoConfiguration;
import com.github.zeng.alt.excel.config.ExcelWebAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code @ExcelImport} / {@code @ExcelExport} 的 MVC 集成测试。
 * <p>
 * {@code @ExcelExport} 在旧版本只有注解、没有处理器，导出功能完全不存在；
 * 这里验证它现在真的能把返回值写成附件，并且能抢在
 * {@code RequestResponseBodyMethodProcessor} 之前接走返回值。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
@SpringBootTest(
        classes = ExcelWebIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ExcelWebIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ExcelTemplate excelTemplate;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void resolvesUploadIntoListParameter() throws Exception {
        MockMultipartFile file = newUpload();

        mockMvc().perform(multipart("/excel/import-list").file(file))
                .andExpect(status().isOk());
    }

    @Test
    void resolvesUploadIntoReadResultParameter() throws Exception {
        MockMultipartFile file = newUpload();

        mockMvc().perform(multipart("/excel/import-result").file(file))
                .andExpect(status().isOk());
    }

    @Test
    void resolvesUploadIntoFlowableParameter() throws Exception {
        MockMultipartFile file = newUpload();

        mockMvc().perform(multipart("/excel/import-stream").file(file))
                .andExpect(status().isOk());
    }

    @Test
    void returnsEmptyWhenOptionalFileIsAbsent() throws Exception {
        mockMvc().perform(multipart("/excel/import-optional"))
                .andExpect(status().isOk());
    }

    @Test
    void writesCollectionReturnValueAsAttachment() throws Exception {
        MvcResult result = mockMvc().perform(post("/excel/export"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentType())
                .startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(result.getResponse().getHeader("Content-Disposition"))
                .contains("attachment")
                .contains("filename*=utf-8''");
        // 真的是一份能读回来的 xlsx，不是空响应
        byte[] body = result.getResponse().getContentAsByteArray();
        assertThat(readBack(body)).extracting(UserRow::getUserName).containsExactly("张三", "李四");
    }

    @Test
    void writesFlowableReturnValueAsAttachment() throws Exception {
        MvcResult result = mockMvc().perform(post("/excel/export-stream"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(readBack(result.getResponse().getContentAsByteArray())).hasSize(3);
    }

    private List<UserRow> readBack(byte[] body) {
        return excelTemplate.read(UserRow.class)
                .from(new ByteArrayInputStream(body))
                .i18nHead(true)
                .execute()
                .rows();
    }

    private MockMultipartFile newUpload() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        excelTemplate.write(UserRow.class)
                .to(output)
                .i18nHead(false)
                .write(List.of(new UserRow("张三", 18), new UserRow("", 30)))
                .get();
        return new MockMultipartFile("file", "users.xlsx",
                MediaType.APPLICATION_OCTET_STREAM_VALUE, output.toByteArray());
    }

    /**
     * 测试用应用与控制器。
     * <p>
     * 不用 {@code @SpringBootApplication}：那会拉起全量自动配置，i18n-component
     * 传递进来的 JPA 会因为没有数据源直接把上下文启动失败。这里只装 MVC 与
     * excel 两个自动配置。
     *
     * @author zengJiaJun
     * @since 2026年09月04日
     * @version 1.0
     */
    @Configuration(proxyBeanMethods = false)
    @RestController
    @ImportAutoConfiguration({
            WebMvcAutoConfiguration.class,
            MultipartAutoConfiguration.class,
            HttpMessageConvertersAutoConfiguration.class,
            JacksonAutoConfiguration.class,
            ValidationAutoConfiguration.class,
            ExcelAutoConfiguration.class,
            ExcelWebAutoConfiguration.class})
    static class TestApplication {

        @PostMapping("/excel/import-list")
        String importList(@ExcelImport("file") List<UserRow> rows) {
            assertThat(rows).hasSize(1);
            assertThat(rows.getFirst().getUserName()).isEqualTo("张三");
            return "ok";
        }

        @PostMapping("/excel/import-result")
        String importResult(@ExcelImport("file") ExcelReadResult<UserRow> result) {
            assertThat(result.rows()).hasSize(1);
            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().getFirst().describe()).contains("姓名不能为空");
            return "ok";
        }

        @PostMapping("/excel/import-stream")
        String importStream(@ExcelImport(value = "file", validate = false) Flowable<UserRow> rows) {
            assertThat(rows.toList().blockingGet()).hasSize(2);
            return "ok";
        }

        @PostMapping("/excel/import-optional")
        String importOptional(@ExcelImport(value = "file", required = false) List<UserRow> rows) {
            assertThat(rows).isEmpty();
            return "ok";
        }

        @PostMapping("/excel/export")
        @ExcelExport(fileName = "用户清单", sheetName = "{excel.test.sheet}", timestamp = false)
        List<UserRow> export() {
            return List.of(new UserRow("张三", 18), new UserRow("李四", 30));
        }

        @PostMapping("/excel/export-stream")
        @ExcelExport(fileName = "flow", type = UserRow.class)
        Flowable<UserRow> exportStream() {
            return Flowable.just(new UserRow("A", 1), new UserRow("B", 2), new UserRow("C", 3));
        }
    }
}
