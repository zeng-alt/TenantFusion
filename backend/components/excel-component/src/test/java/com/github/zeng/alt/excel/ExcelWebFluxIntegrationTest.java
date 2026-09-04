package com.github.zeng.alt.excel;

import com.github.zeng.alt.excel.annotation.ExcelExport;
import com.github.zeng.alt.excel.annotation.ExcelImport;
import com.github.zeng.alt.excel.config.ExcelAutoConfiguration;
import com.github.zeng.alt.excel.config.ExcelWebAutoConfiguration;
import com.github.zeng.alt.excel.config.ExcelWebFluxAutoConfiguration;
import com.github.zeng.alt.excel.read.ExcelReadResult;
import io.reactivex.rxjava3.core.Flowable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.codec.CodecsAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WebFlux 栈的 {@code @ExcelImport} / {@code @ExcelExport} 集成测试。
 * <p>
 * 旧版本只有 Servlet 集成，WebFlux 应用里两个注解静默失效；这里验证响应式栈
 * 四种导入形状与五种导出形状都通，且导出的响应头与 Servlet 栈一致。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
// 测试 classpath 上 web 与 webflux 两个 starter 都在，必须显式钉死响应式栈，
// 否则 Spring Boot 按 classpath 推断会选 Servlet
@SpringBootTest(
        classes = ExcelWebFluxIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.main.web-application-type=reactive")
class ExcelWebFluxIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ExcelTemplate excelTemplate;

    @Test
    void resolvesUploadIntoListParameter() {
        webTestClient.post().uri("/excel/import-list")
                .body(upload())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void resolvesUploadIntoReadResultParameter() {
        webTestClient.post().uri("/excel/import-result")
                .body(upload())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void resolvesUploadIntoFluxParameter() {
        webTestClient.post().uri("/excel/import-flux")
                .body(upload())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void resolvesUploadIntoFlowableParameter() {
        webTestClient.post().uri("/excel/import-flowable")
                .body(upload())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void returnsEmptyWhenOptionalFileIsAbsent() {
        webTestClient.post().uri("/excel/import-optional")
                .body(BodyInserters.fromMultipartData(new MultipartBodyBuilder().build()))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void writesCollectionReturnValueAsAttachment() {
        byte[] body = webTestClient.post().uri("/excel/export")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueMatches(HttpHeaders.CONTENT_TYPE,
                        "application/vnd\\.openxmlformats-officedocument\\.spreadsheetml\\.sheet.*")
                .expectHeader().valueMatches(HttpHeaders.CONTENT_DISPOSITION, "attachment.*filename\\*=utf-8''.*")
                .expectBody().returnResult().getResponseBody();

        assertThat(readBack(body)).extracting(UserRow::getUserName).containsExactly("张三", "李四");
    }

    @Test
    void writesFluxReturnValueAsAttachment() {
        byte[] body = webTestClient.post().uri("/excel/export-flux")
                .exchange()
                .expectStatus().isOk()
                .expectBody().returnResult().getResponseBody();

        assertThat(readBack(body)).hasSize(3);
    }

    @Test
    void writesMonoReturnValueAsAttachment() {
        byte[] body = webTestClient.post().uri("/excel/export-mono")
                .exchange()
                .expectStatus().isOk()
                .expectBody().returnResult().getResponseBody();

        assertThat(readBack(body)).hasSize(1);
    }

    @Test
    void writesFlowableReturnValueAsAttachment() {
        byte[] body = webTestClient.post().uri("/excel/export-flowable")
                .exchange()
                .expectStatus().isOk()
                .expectBody().returnResult().getResponseBody();

        assertThat(readBack(body)).hasSize(2);
    }

    private List<UserRow> readBack(byte[] body) {
        return excelTemplate.read(UserRow.class)
                .from(new ByteArrayInputStream(body))
                .i18nHead(true)
                .execute()
                .rows();
    }

    private BodyInserters.MultipartInserter upload() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        excelTemplate.write(UserRow.class)
                .to(output)
                .i18nHead(false)
                .write(List.of(new UserRow("张三", 18), new UserRow("", 30)))
                .get();

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new ByteArrayResource(output.toByteArray())).filename("users.xlsx");
        return BodyInserters.fromMultipartData(builder.build());
    }

    /**
     * 测试用应用与控制器。
     * <p>
     * 只装 WebFlux 与 excel 的自动配置，不用 {@code @SpringBootApplication}——
     * 那会拉起全量自动配置，i18n-component 传递进来的 JPA 会因为没有数据源
     * 直接把上下文启动失败。
     *
     * @author zengJiaJun
     * @since 2026年09月04日
     * @version 1.0
     */
    @Configuration(proxyBeanMethods = false)
    @RestController
    @ImportAutoConfiguration({
            ReactiveWebServerFactoryAutoConfiguration.class,
            HttpHandlerAutoConfiguration.class,
            WebFluxAutoConfiguration.class,
            CodecsAutoConfiguration.class,
            JacksonAutoConfiguration.class,
            ValidationAutoConfiguration.class,
            ExcelAutoConfiguration.class,
            ExcelWebAutoConfiguration.class,
            ExcelWebFluxAutoConfiguration.class})
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

        @PostMapping("/excel/import-flux")
        Mono<String> importFlux(@ExcelImport(value = "file", validate = false) Flux<UserRow> rows) {
            return rows.collectList().map(list -> {
                assertThat(list).hasSize(2);
                return "ok";
            });
        }

        @PostMapping("/excel/import-flowable")
        String importFlowable(@ExcelImport(value = "file", validate = false) Flowable<UserRow> rows) {
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

        @PostMapping("/excel/export-flux")
        @ExcelExport(fileName = "flux")
        Flux<UserRow> exportFlux() {
            return Flux.just(new UserRow("A", 1), new UserRow("B", 2), new UserRow("C", 3));
        }

        @PostMapping("/excel/export-mono")
        @ExcelExport(fileName = "mono", type = UserRow.class)
        Mono<List<UserRow>> exportMono() {
            return Mono.just(List.of(new UserRow("only", 1)));
        }

        @PostMapping("/excel/export-flowable")
        @ExcelExport(fileName = "flowable", type = UserRow.class)
        Flowable<UserRow> exportFlowable() {
            return Flowable.just(new UserRow("A", 1), new UserRow("B", 2));
        }
    }
}
