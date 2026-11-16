package com.github.zeng.alt.excel;

import com.github.zeng.alt.excel.annotation.ExcelExport;
import com.github.zeng.alt.excel.annotation.ExcelImport;
import com.github.zeng.alt.excel.config.ExcelProperties;
import com.github.zeng.alt.excel.context.ExcelContextTemplate;
import com.github.zeng.alt.excel.read.ExcelReadResult;
import com.github.zeng.alt.excel.read.ExcelReadSpec;
import com.github.zeng.alt.excel.web.ExcelWebSpecFactory;
import com.github.zeng.alt.excel.web.RxJavaExcelReactiveSupport;
import com.github.zeng.alt.excel.web.reactive.ExcelExportResultHandler;
import com.github.zeng.alt.excel.web.reactive.ExcelImportReactiveArgumentResolver;
import com.github.zeng.alt.excel.write.ExcelWriteSpec;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import io.reactivex.rxjava3.core.Flowable;
import io.vavr.control.Try;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.MethodParameter;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * WebFlux 的 Reactor 安全上下文与调用线程租户在阻塞边界合并。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class ExcelWebFluxContextTest {

    @TempDir
    Path directory;

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
        UserContextHolder.clear();
        LocaleContextHolder.resetLocaleContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {"list", "flux", "flowable"})
    @SuppressWarnings("unchecked")
    void restoresReactorAuthenticationAndCapturedTenantForImport(String shape) throws Exception {
        final Authentication authentication = captureCaller();
        final ExcelWebSpecFactory factory = factory();
        final ExcelReadSpec<Object> spec = mock(ExcelReadSpec.class, RETURNS_SELF);
        when(factory.readSpec(any(), any())).thenAnswer(invocation -> {
            ExcelContextTemplateTest.assertContext("caller");
            return spec;
        });
        when(spec.execute()).thenAnswer(invocation -> ExcelReadResult.of(List.of(new UserRow("A", 1))));
        when(spec.consumeWhile(any())).thenAnswer(invocation -> {
            ExcelContextTemplateTest.assertContext("caller");
            final Predicate<Object> consumer = invocation.getArgument(0);
            consumer.test(new UserRow("A", 1));
            return Try.success(1L);
        });
        final ExcelImportReactiveArgumentResolver resolver = new ExcelImportReactiveArgumentResolver(
                factory, properties(), new RxJavaExcelReactiveSupport()
        );
        final Mono<Object> argument = resolver.resolveArgument(parameter(shape), new BindingContext(), exchange());
        ExcelContextTemplateTest.setContext("later");

        final Object value = argument.contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
                .block(Duration.ofSeconds(5));
        if (value instanceof Flux<?> flux) {
            assertThat(flux.doOnNext(row -> ExcelContextTemplateTest.assertContext("caller"))
                    .collectList().block(Duration.ofSeconds(5))).hasSize(1);
        } else if (value instanceof Flowable<?> flowable) {
            assertThat(flowable.doOnNext(row -> ExcelContextTemplateTest.assertContext("caller"))
                    .toList().blockingGet()).hasSize(1);
        } else {
            assertThat((List<?>) value).hasSize(1);
        }
        ExcelContextTemplateTest.assertContext("later");
    }

    @Test
    @SuppressWarnings("unchecked")
    void restoresContextWhileExportingAndPreservesCaller() throws Exception {
        final Authentication authentication = captureCaller();
        final ExcelWebSpecFactory factory = factory();
        final ExcelWriteSpec<Object> spec = mock(ExcelWriteSpec.class, RETURNS_SELF);
        when(factory.writeSpec(any(), any())).thenAnswer(invocation -> {
            ExcelContextTemplateTest.assertContext("caller");
            return spec;
        });
        when(spec.write(any(Iterator.class))).thenAnswer(invocation -> {
            final Iterator<?> rows = invocation.getArgument(0);
            rows.forEachRemaining(row -> ExcelContextTemplateTest.assertContext("caller"));
            return Try.success(1L);
        });
        final ExcelExportResultHandler handler = new ExcelExportResultHandler(
                factory, new RxJavaExcelReactiveSupport(), properties()
        );
        final HandlerResult result = new HandlerResult(this, Flux.just(new UserRow("A", 1)),
                new MethodParameter(getClass().getDeclaredMethod("export"), -1));
        final Mono<Void> response = handler.handleResult(
                MockServerWebExchange.from(MockServerHttpRequest.get("/export")), result
        );
        ExcelContextTemplateTest.setContext("later");

        response.contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
                .block(Duration.ofSeconds(5));

        ExcelContextTemplateTest.assertContext("later");
        try (Stream<Path> files = Files.list(directory)) {
            assertThat(files).isEmpty();
        }
    }

    private Authentication captureCaller() {
        ExcelContextTemplateTest.setContext("caller");
        final Authentication authentication = UserContextHolder.getAuthentication();
        UserContextHolder.clear();
        return authentication;
    }

    private ExcelWebSpecFactory factory() {
        final ExcelWebSpecFactory factory = mock(ExcelWebSpecFactory.class);
        when(factory.captureContext()).thenAnswer(invocation -> ExcelContextTemplate.captureDefaults());
        when(factory.fileNameTimestampPattern()).thenReturn("yyyyMMdd");
        return factory;
    }

    private ExcelProperties properties() {
        final ExcelProperties properties = new ExcelProperties();
        properties.getWeb().setTempDir(directory.toString());
        return properties;
    }

    private MethodParameter parameter(String shape) throws Exception {
        final Class<?> type = switch (shape) {
            case "list" -> List.class;
            case "flux" -> Flux.class;
            default -> Flowable.class;
        };
        return new MethodParameter(getClass().getDeclaredMethod(shape, type), 0);
    }

    private ServerWebExchange exchange() {
        final FilePart part = mock(FilePart.class);
        when(part.transferTo(any(Path.class))).thenReturn(Mono.empty());
        final LinkedMultiValueMap<String, Part> parts = new LinkedMultiValueMap<>();
        parts.add("file", part);
        final ServerWebExchange exchange = mock(ServerWebExchange.class);
        when(exchange.getMultipartData()).thenReturn(Mono.just(parts));
        return exchange;
    }

    private void list(@ExcelImport("file") List<UserRow> rows) {
    }

    private void flux(@ExcelImport("file") Flux<UserRow> rows) {
    }

    private void flowable(@ExcelImport("file") Flowable<UserRow> rows) {
    }

    @ExcelExport(type = UserRow.class)
    private Flux<UserRow> export() {
        return Flux.empty();
    }
}
