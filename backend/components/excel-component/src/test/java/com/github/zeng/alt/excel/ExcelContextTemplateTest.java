package com.github.zeng.alt.excel;

import com.github.zeng.alt.excel.config.ExcelAutoConfiguration;
import com.github.zeng.alt.excel.context.ExcelContextHandler;
import com.github.zeng.alt.excel.context.ExcelContextTemplate;
import com.github.zeng.alt.excel.context.SecurityExcelContextHandler;
import com.github.zeng.alt.excel.context.TenantExcelContextHandler;
import com.github.zeng.alt.excel.read.ExcelReadSpec;
import com.github.zeng.alt.excel.rx.RxExcel;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.tenant.api.TenantContextHolder;
import com.github.zeng.alt.tenant.api.TenantRouting;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 租户、用户、安全、语言上下文在异步执行和异常退出后的隔离。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class ExcelContextTemplateTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ExcelAutoConfiguration.class));

    @AfterEach
    void clearCaller() {
        TenantContextHolder.clear();
        UserContextHolder.clear();
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void restoresAllHoldersAndPreviousStateAfterFailure() {
        setContext("caller");
        final ExcelContextHandler.Snapshot snapshot = ExcelContextTemplate.captureDefaults();
        setContext("worker");
        final SecurityContext previous = SecurityContextHolder.getContext();

        assertThatThrownBy(() -> snapshot.call(() -> {
            assertContext("caller");
            assertThat(SecurityContextHolder.getContext()).isNotSameAs(previous);
            throw new IllegalStateException("business failure");
        })).isInstanceOf(IllegalStateException.class);

        assertContext("worker");
        assertThat(SecurityContextHolder.getContext()).isSameAs(previous);
    }

    @Test
    void emptySnapshotClearsWorkerIdentityOnlyWithinScope() {
        final ExcelContextHandler.Snapshot snapshot = ExcelContextTemplate.captureDefaults();
        setContext("worker");

        snapshot.decorate(() -> {
            assertThat(TenantContextHolder.getTenantId()).isNull();
            assertThat(UserContextHolder.getId()).isNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }).run();

        assertContext("worker");
    }

    @Test
    void rxReadsWithCapturedContextAfterCallerHasChanged() {
        runner.run(context -> {
            final ExcelTemplate template = context.getBean(ExcelTemplate.class);
            final byte[] bytes = workbook(template);
            setContext("caller");
            final ExcelReadSpec<UserRow> spec = template.read(UserRow.class)
                    .from(new ByteArrayInputStream(bytes));
            setContext("later");

            final List<UserRow> rows = RxExcel.stream(spec).doOnNext(row -> assertContext("caller"))
                    .toList().blockingGet();

            assertThat(rows).hasSize(2);
            assertContext("later");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"complete", "error", "cancel"})
    void explicitObserveOnBoundaryRestoresReusedWorker(String outcome) {
        final Scheduler scheduler = Schedulers.single();
        try {
            Single.fromCallable(() -> { setContext("worker"); return true; }).subscribeOn(scheduler).blockingGet();
            setContext("caller");
            final ExcelContextHandler.Snapshot snapshot = ExcelContextTemplate.captureDefaults();

            final Flowable<Integer> rows = RxExcel.withContext(Flowable.just(1, 2).observeOn(scheduler), snapshot)
                    .doOnNext(row -> assertContext("caller"));
            if ("error".equals(outcome)) {
                assertThatThrownBy(() -> rows.doOnNext(row -> {
                    throw new IllegalStateException("business failure");
                }).toList().blockingGet()).isInstanceOf(IllegalStateException.class);
            } else {
                assertThat(("cancel".equals(outcome) ? rows.take(1) : rows).toList().blockingGet()).isNotEmpty();
            }

            Single.fromCallable(() -> { assertContext("worker"); return true; }).subscribeOn(scheduler).blockingGet();
        } finally {
            Single.fromCallable(() -> { clearCaller(); return true; }).subscribeOn(scheduler).blockingGet();
        }
    }

    @Test
    void contributesCustomContextAndHonorsDisableSwitch() {
        final ThreadLocal<String> trace = new ThreadLocal<>();
        final ExcelContextHandler handler = () -> {
            final String captured = trace.get();
            return () -> {
                final String previous = trace.get();
                trace.set(captured);
                return () -> trace.set(previous);
            };
        };
        runner.withBean("traceExcelContextHandler", ExcelContextHandler.class, () -> handler).run(context -> {
            trace.set("request");
            final ExcelContextHandler.Snapshot snapshot = context.getBean(ExcelTemplate.class).captureContext();
            trace.set("worker");
            snapshot.decorate(() -> assertThat(trace.get()).isEqualTo("request")).run();
            assertThat(trace.get()).isEqualTo("worker");
        });
        runner.withPropertyValues("alt.excel.context.enabled=false").run(context -> {
            setContext("caller");
            final ExcelContextHandler.Snapshot snapshot = context.getBean(ExcelTemplate.class).captureContext();
            setContext("worker");
            snapshot.decorate(() -> assertContext("worker")).run();
        });
        trace.remove();
    }

    @Test
    void optionalTenantAndSecurityDependenciesCanBeAbsent() {
        runner.withClassLoader(new FilteredClassLoader("com.github.zeng.alt.tenant", "org.springframework.security"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(TenantExcelContextHandler.class);
                    assertThat(context).doesNotHaveBean(SecurityExcelContextHandler.class);
                    assertThat(context).hasSingleBean(ExcelTemplate.class);
                });
    }

    private static byte[] workbook(ExcelTemplate template) {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        template.write(UserRow.class).to(output).i18nHead(false)
                .write(List.of(new UserRow("A", 10), new UserRow("B", 20))).get();
        return output.toByteArray();
    }

    static void setContext(String name) {
        TenantContextHolder.setTenantId(name);
        TenantContextHolder.setDatabase(name + "_db");
        TenantContextHolder.setSchema(name + "_schema");
        TenantContextHolder.setRouting(new TenantRouting(name, name + "_db", name + "_schema", "_suffix", true));
        final SecurityUser user = SecurityUser.withUsername(name).id(name + "_id")
                .authorities(new SimpleGrantedAuthority("ROLE_IMPORT")).build();
        UserContextHolder.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities())
        );
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
    }

    static void assertContext(String name) {
        assertThat(TenantContextHolder.getTenantId()).isEqualTo(name);
        assertThat(TenantContextHolder.getDatabase()).isEqualTo(name + "_db");
        assertThat(TenantContextHolder.getSchema()).isEqualTo(name + "_schema");
        assertThat(TenantContextHolder.getRouting().tableSuffix()).isEqualTo("_suffix");
        assertThat(UserContextHolder.getId()).isEqualTo(name + "_id");
        assertThat(UserContextHolder.getUsername()).isEqualTo(name);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString).containsExactly("ROLE_IMPORT");
        assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.SIMPLIFIED_CHINESE);
    }
}
