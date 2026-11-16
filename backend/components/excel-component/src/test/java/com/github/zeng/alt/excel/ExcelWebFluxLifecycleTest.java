package com.github.zeng.alt.excel;

import com.github.zeng.alt.excel.annotation.ExcelExport;
import com.github.zeng.alt.excel.annotation.ExcelImport;
import com.github.zeng.alt.excel.config.ExcelProperties;
import com.github.zeng.alt.excel.read.ExcelReadSpec;
import com.github.zeng.alt.excel.web.NoOpExcelReactiveSupport;
import com.github.zeng.alt.excel.web.ExcelWebSpecFactory;
import com.github.zeng.alt.excel.web.reactive.ExcelExportResultHandler;
import com.github.zeng.alt.excel.web.reactive.ExcelImportReactiveArgumentResolver;
import com.github.zeng.alt.excel.write.ExcelWriteSpec;
import io.vavr.control.Try;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.MethodParameter;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.Disposable;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * WebFlux 导入导出的流量控制与临时文件生命周期。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class ExcelWebFluxLifecycleTest {

    @TempDir
    Path directory;

    @Test
    @SuppressWarnings("unchecked")
    void failedExportCancelsPublisherWithoutDrainingIt() throws Exception {
        final ExcelWebSpecFactory factory = mock(ExcelWebSpecFactory.class);
        final ExcelWriteSpec<Object> spec = mock(ExcelWriteSpec.class, RETURNS_SELF);
        when(factory.fileNameTimestampPattern()).thenReturn("yyyyMMdd");
        when(factory.writeSpec(any(), any())).thenReturn(spec);
        final AtomicInteger pulled = new AtomicInteger();
        final AtomicBoolean cancelled = new AtomicBoolean();
        when(spec.write(any(Iterator.class))).thenAnswer(invocation -> {
            final Iterator<?> iterator = invocation.getArgument(0);
            assertThat(iterator.hasNext()).isTrue();
            iterator.next();
            return Try.failure(new IOException("disk full"));
        });
        when(spec.write(any(java.util.Collection.class))).thenReturn(Try.failure(new IOException("disk full")));
        final Flux<UserRow> rows = Flux.range(0, 10_000)
                .map(i -> new UserRow("u" + i, 1))
                .doOnNext(row -> pulled.incrementAndGet())
                .doOnCancel(() -> cancelled.set(true));
        final ExcelExportResultHandler handler = exportHandler(factory);
        final MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/export"));

        StepVerifier.create(handler.handleResult(exchange, exportResult(rows)))
                .expectError().verify(Duration.ofSeconds(5));

        assertThat(pulled.get()).isLessThanOrEqualTo(128);
        assertThat(cancelled).isTrue();
        assertDirectoryEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void iteratorIsReadOnlyAfterSubscriptionAndOffCallerThread() throws Exception {
        final ExcelWebSpecFactory factory = mock(ExcelWebSpecFactory.class);
        final ExcelWriteSpec<Object> spec = mock(ExcelWriteSpec.class, RETURNS_SELF);
        when(factory.fileNameTimestampPattern()).thenReturn("yyyyMMdd");
        when(factory.writeSpec(any(), any())).thenReturn(spec);
        final AtomicReference<Thread> reader = new AtomicReference<>();
        final Iterator<UserRow> rows = Stream.of(new UserRow("A", 1))
                .peek(row -> reader.set(Thread.currentThread())).iterator();
        when(spec.write(any(Iterator.class))).thenAnswer(invocation -> {
            final Iterator<?> iterator = invocation.getArgument(0);
            iterator.forEachRemaining(row -> { });
            return Try.success(1L);
        });
        final ExcelExportResultHandler handler = exportHandler(factory);
        final MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/export"));
        final Mono<Void> response = handler.handleResult(exchange, exportResult(rows));

        assertThat(reader.get()).isNull();
        response.block(Duration.ofSeconds(5));
        assertThat(reader.get()).isNotNull().isNotSameAs(Thread.currentThread());
        assertDirectoryEmpty();
    }

    @Test
    void failedUploadRemovesPartialFile() throws Exception {
        final FilePart part = mock(FilePart.class);
        when(part.transferTo(any(Path.class))).thenAnswer(invocation -> Mono.defer(() -> {
            final Path path = invocation.getArgument(0);
            Try.run(() -> Files.writeString(path, "partial")).get();
            return Mono.error(new IOException("upload interrupted"));
        }));

        StepVerifier.create(resolver(mock(ExcelWebSpecFactory.class)).resolveArgument(
                parameter("importList", List.class), new BindingContext(), exchange(part)
        )).expectError().verify(Duration.ofSeconds(5));

        assertDirectoryEmpty();
    }

    @Test
    void cancelledUploadRemovesPartialFile() throws Exception {
        final FilePart part = mock(FilePart.class);
        final CountDownLatch transferring = new CountDownLatch(1);
        when(part.transferTo(any(Path.class))).thenAnswer(invocation -> Mono.defer(() -> {
            transferring.countDown();
            return Mono.never();
        }));
        final Disposable subscription = resolver(mock(ExcelWebSpecFactory.class)).resolveArgument(
                parameter("importList", List.class), new BindingContext(), exchange(part)
        ).subscribe();

        assertThat(transferring.await(5, TimeUnit.SECONDS)).isTrue();
        subscription.dispose();
        await().untilAsserted(this::assertDirectoryEmpty);
    }

    @Test
    @SuppressWarnings("unchecked")
    void cancellingExportClosesCursorAndRemovesFile() throws Exception {
        final ExcelWebSpecFactory factory = mock(ExcelWebSpecFactory.class);
        final ExcelWriteSpec<Object> spec = mock(ExcelWriteSpec.class, RETURNS_SELF);
        when(factory.fileNameTimestampPattern()).thenReturn("yyyyMMdd");
        when(factory.writeSpec(any(), any())).thenReturn(spec);
        final CountDownLatch reading = new CountDownLatch(1);
        final AtomicBoolean cancelled = new AtomicBoolean();
        when(spec.write(any(Iterator.class))).thenAnswer(invocation -> Try.of(() -> {
            final Iterator<?> iterator = invocation.getArgument(0);
            assertThat(iterator.hasNext()).isTrue();
            iterator.next();
            reading.countDown();
            iterator.hasNext();
            return 1L;
        }));
        final Flux<UserRow> rows = Flux.just(new UserRow("A", 1))
                .concatWith(Flux.never()).doOnCancel(() -> cancelled.set(true));
        final Disposable subscription = exportHandler(factory).handleResult(
                MockServerWebExchange.from(MockServerHttpRequest.get("/export")), exportResult(rows)
        ).subscribe();

        assertThat(reading.await(5, TimeUnit.SECONDS)).isTrue();
        subscription.dispose();

        await().untilTrue(cancelled);
        await().untilAsserted(this::assertDirectoryEmpty);
    }

    @Test
    @SuppressWarnings("unchecked")
    void fluxImportWaitsForDemandAndCleansUpOnCancel() throws Exception {
        final ExcelWebSpecFactory factory = mock(ExcelWebSpecFactory.class);
        final ExcelReadSpec<Object> spec = mock(ExcelReadSpec.class, RETURNS_SELF);
        when(factory.readSpec(any(), any())).thenAnswer(invocation -> spec);
        final AtomicInteger parsed = new AtomicInteger();
        when(spec.consumeWhile(any())).thenAnswer(invocation -> {
            final Predicate<Object> consumer = invocation.getArgument(0);
            for (int index = 0; index < 1000; index++) {
                parsed.incrementAndGet();
                if (!consumer.test(new UserRow("u" + index, 1))) {
                    break;
                }
            }
            return Try.success((long) parsed.get());
        });
        final FilePart part = mock(FilePart.class);
        when(part.transferTo(any(Path.class))).thenReturn(Mono.empty());
        final Flux<Object> rows = (Flux<Object>) resolver(factory).resolveArgument(
                parameter("importFlux", Flux.class), new BindingContext(), exchange(part)
        ).block(Duration.ofSeconds(5));

        StepVerifier.create(rows, 0)
                .thenAwait(Duration.ofMillis(100))
                .then(() -> assertThat(parsed.get()).isLessThanOrEqualTo(1))
                .thenRequest(2).expectNextCount(2).thenCancel().verify(Duration.ofSeconds(5));
        await().untilAsserted(this::assertDirectoryEmpty);
    }

    private ExcelExportResultHandler exportHandler(ExcelWebSpecFactory factory) {
        final ExcelProperties properties = new ExcelProperties();
        properties.getWeb().setTempDir(directory.toString());
        return new ExcelExportResultHandler(factory, new NoOpExcelReactiveSupport(), properties);
    }

    private ExcelImportReactiveArgumentResolver resolver(ExcelWebSpecFactory factory) {
        final ExcelProperties properties = new ExcelProperties();
        properties.getWeb().setTempDir(directory.toString());
        return new ExcelImportReactiveArgumentResolver(factory, properties, new NoOpExcelReactiveSupport());
    }

    private static ServerWebExchange exchange(FilePart part) {
        final ServerWebExchange exchange = mock(ServerWebExchange.class);
        final LinkedMultiValueMap<String, Part> parts = new LinkedMultiValueMap<>();
        parts.add("file", part);
        when(exchange.getMultipartData()).thenReturn(Mono.just(parts));
        return exchange;
    }

    private static MethodParameter parameter(String name, Class<?> type) throws Exception {
        return new MethodParameter(ExcelWebFluxLifecycleTest.class.getDeclaredMethod(name, type), 0);
    }

    private HandlerResult exportResult(Object rows) throws Exception {
        return new HandlerResult(this, rows, new MethodParameter(getClass().getDeclaredMethod("export"), -1));
    }

    private void assertDirectoryEmpty() throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            assertThat(files).isEmpty();
        }
    }

    @ExcelExport(type = UserRow.class)
    private Flux<UserRow> export() {
        return Flux.empty();
    }

    private void importList(@ExcelImport("file") List<UserRow> users) {
    }

    private void importFlux(@ExcelImport("file") Flux<UserRow> users) {
    }
}
