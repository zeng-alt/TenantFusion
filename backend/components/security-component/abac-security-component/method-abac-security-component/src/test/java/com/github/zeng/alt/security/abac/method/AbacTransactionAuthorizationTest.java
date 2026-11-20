package com.github.zeng.alt.security.abac.method;

import com.github.zeng.alt.security.abac.AbacAuthorize;
import com.github.zeng.alt.security.abac.AbacPolicy;
import com.github.zeng.alt.security.abac.AbacTemplate;
import com.github.zeng.alt.security.abac.core.AbacProperties;
import com.github.zeng.alt.security.abac.core.AbacTemplateImpl;
import com.github.zeng.alt.security.abac.spel.SpelAbacEvaluator;
import io.vavr.control.Either;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 实际 Spring 事务和 H2 写入：方法已经执行，但后置拒绝必须撤销全部修改。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class AbacTransactionAuthorizationTest {
    @Test
    void rejectsAfterMethodAndRollsBackTheWrite() {
        try (final var context = context()) {
            assertThatThrownBy(() -> context.getBean(Writer.class).update("rejected"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThat(context.getBean(AtomicInteger.class)).hasValue(1);
            assertThat(value(context)).isEqualTo("original");
        }
    }

    @Test
    void permitsPostConditionAndCommitsTheWrite() {
        try (final var context = context()) {
            context.getBean(AtomicBoolean.class).set(true);
            assertThat(context.getBean(Writer.class).update("allowed").allowed()).isTrue();
            assertThat(value(context)).isEqualTo("allowed");
        }
    }

    @Test
    void errorsWithoutTransactionBeforeInvokingTheMethod() {
        try (final var context = context()) {
            assertThatThrownBy(() -> context.getBean(Writer.class).withoutTransaction())
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("事务");
            assertThat(context.getBean(AtomicInteger.class)).hasValue(0);
            assertThat(value(context)).isEqualTo("original");
        }
    }

    @Test
    void voidWritesAlsoRunPostAuthorizationAndRollBack() {
        try (final var context = context()) {
            assertThatThrownBy(() -> context.getBean(Writer.class).updateWithoutResult())
                    .isInstanceOf(AccessDeniedException.class);
            assertThat(value(context)).isEqualTo("original");
        }
    }

    private AnnotationConfigApplicationContext context() {
        final var context = new AnnotationConfigApplicationContext(Application.class);
        context.getBean(JdbcTemplate.class).execute("create table changes (id bigint primary key, text_value varchar(64))");
        context.getBean(JdbcTemplate.class).update("insert into changes values (1, 'original')");
        return context;
    }

    private String value(AnnotationConfigApplicationContext context) {
        return context.getBean(JdbcTemplate.class).queryForObject("select text_value from changes where id=1", String.class);
    }

    @Configuration(proxyBeanMethods = false)
    @Import(AbacMethodAutoConfiguration.class)
    static class Application {
        @Bean(destroyMethod = "shutdown")
        EmbeddedDatabase database() {
            return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
        }
        @Bean
        JdbcTemplate jdbc(EmbeddedDatabase database) { return new JdbcTemplate(database); }
        @Bean
        DataSourceTransactionManager transactionManager(EmbeddedDatabase database) {
            return new DataSourceTransactionManager(database);
        }
        @Bean
        AtomicBoolean allowed() { return new AtomicBoolean(); }
        @Bean
        AtomicInteger methodCalls() { return new AtomicInteger(); }
        @Bean
        Writer writer(JdbcTemplate jdbc, AtomicBoolean allowed, AtomicInteger calls) { return new Writer(jdbc, allowed, calls); }
        @Bean
        SpelAbacEvaluator evaluator() { return new SpelAbacEvaluator(); }
        @Bean
        AbacTemplate template(SpelAbacEvaluator evaluator) {
            return new AbacTemplateImpl(context -> List.of(new AbacPolicy("1",
                "t001",
                context.permission(),
                context.permission().equals("change:void") ? "false" : "returnObject.allowed",
                false)), evaluator, new AbacProperties());
        }
        @Bean
        AbacSubjectProvider subjects() {
            return permission -> Either.right(new AbacSubject("t001", Map.of(), Map.of("authenticated", true)));
        }
        @Bean
        AbacEnvironmentProvider environment() { return context -> Map.of(); }
    }

    static class Writer {
        private final JdbcTemplate jdbc;
        private final AtomicBoolean allowed;
        private final AtomicInteger calls;
        Writer(JdbcTemplate jdbc, AtomicBoolean allowed, AtomicInteger calls) {
            this.jdbc = jdbc;
            this.allowed = allowed;
            this.calls = calls;
        }
        @Transactional
        @AbacAuthorize(permission = "change", requirePolicy = true)
        public Reply update(String value) {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            calls.incrementAndGet();
            jdbc.update("update changes set text_value=? where id=1", value);
            return new Reply(allowed.get());
        }
        @AbacAuthorize(permission = "change", requirePolicy = true)
        public Reply withoutTransaction() {
            calls.incrementAndGet();
            jdbc.update("update changes set text_value='invalid' where id=1");
            return new Reply(true);
        }
        @Transactional
        @AbacAuthorize(permission = "change:void", requirePolicy = true)
        public void updateWithoutResult() {
            jdbc.update("update changes set text_value='invalid' where id=1");
        }
    }

    record Reply(boolean allowed) { }
}
