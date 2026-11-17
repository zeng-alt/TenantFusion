package com.github.zeng.alt.log;

import com.github.zeng.alt.json.JsonConfiguration;
import com.github.zeng.alt.event.EventTemplate;
import com.github.zeng.alt.log.message.config.MessageLogAutoConfiguration;
import com.github.zeng.alt.log.message.consumer.MessageLogConsumer;
import com.github.zeng.alt.log.core.config.LogAutoConfiguration;
import com.github.zeng.alt.log.jpa.config.JpaLogAutoConfiguration;
import com.github.zeng.alt.log.jpa.repository.LogRepository;
import com.github.zeng.alt.log.jpa.repository.LoginLogRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 通过真实 H2/JPA 事务验证日志提交与业务回滚隔离。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class JpaLogIntegrationTest {
    @Test
    void storesEventsWithoutAnExistingTransaction() {
        runner().run(context -> {
            assertThat(context).hasNotFailed();
            final OperLogEvent event = new OperLogEvent();
            event.setTitle("无事务");
            event.setTenantId("tenant-a");
            event.setStatus(0);
            event.setOperTime(java.time.LocalDateTime.now());
            context.publishEvent(event);
            final var saved = context.getBean(LogRepository.class).findAll().getFirst();
            assertThat(saved.getTitle()).isEqualTo("无事务");
            assertThat(saved.getTenantId()).isEqualTo("tenant-a");
            assertThat(saved.getOperTime()).isNotNull();
            final LoginInfoEvent login = new LoginInfoEvent();
            login.setUsername("alice");
            login.setStatus("0");
            context.publishEvent(login);
            assertThat(context.getBean(LoginLogRepository.class).findAll()).hasSize(1);
        });
    }

    @Test
    void recordsSuccessAndFailureOnTransactionalMethods() {
        runner().run(context -> {
            final var actions = context.getBean(Actions.class);
            actions.succeed();
            assertThatThrownBy(actions::fail).hasMessage("business failed");
            final var records = context.getBean(LogRepository.class).findAll();
            assertThat(records).hasSize(2);
            assertThat(records).extracting(record -> record.getStatus()).containsExactlyInAnyOrder(0, 1);
        });
    }

    @Test
    void preservesLogsWhenAnOuterTransactionRollsBack() {
        runner().run(context -> {
            final var transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
            transaction.executeWithoutResult(status -> {
                context.getBean(Actions.class).succeed();
                final LoginInfoEvent login = new LoginInfoEvent();
                login.setUsername("alice");
                context.publishEvent(login);
                status.setRollbackOnly();
            });
            assertThat(context.getBean(LogRepository.class).findAll()).hasSize(1);
            assertThat(context.getBean(LoginLogRepository.class).findAll()).hasSize(1);
        });
    }

    @Test
    void exposesPersistenceFailureAndAllowsTheNextEventToSucceed() {
        runner().run(context -> {
            final OperLogEvent invalid = new OperLogEvent();
            invalid.setTitle("x".repeat(300));
            assertThatThrownBy(() -> context.publishEvent(invalid)).isInstanceOf(RuntimeException.class);
            assertThat(context.getBean(LogRepository.class).findAll()).isEmpty();
            final OperLogEvent valid = new OperLogEvent();
            valid.setTitle("valid");
            context.publishEvent(valid);
            assertThat(context.getBean(LogRepository.class).findAll()).hasSize(1);
        });
    }

    @Test
    void storesConsumedMessagesAndPropagatesDatabaseFailures() {
        runner().withConfiguration(AutoConfigurations.of(MessageLogAutoConfiguration.class))
                .withBean(EventTemplate.class, () -> org.mockito.Mockito.mock(EventTemplate.class))
                .withPropertyValues("log.message.producer-enabled=false", "log.message.consumer-enabled=true")
                .run(context -> {
                    final var consumer = context.getBean(MessageLogConsumer.class);
                    final OperLogEvent invalid = new OperLogEvent();
                    invalid.setTitle("x".repeat(300));
                    assertThatThrownBy(() -> consumer.onOperLog(invalid)).isInstanceOf(RuntimeException.class);
                    final OperLogEvent valid = new OperLogEvent();
                    valid.setTitle("consumed");
                    consumer.onOperLog(valid);
                    assertThat(context.getBean(LogRepository.class).findAll().getFirst().getTitle())
                            .isEqualTo("consumed");
                });
    }

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                DataJpaRepositoriesAutoConfiguration.class,
                TransactionAutoConfiguration.class,
                JsonConfiguration.class,
                JacksonAutoConfiguration.class,
                LogAutoConfiguration.class,
                JpaLogAutoConfiguration.class
        )).withUserConfiguration(Fixtures.class).withPropertyValues(
                "spring.datasource.url=jdbc:h2:mem:log_" + UUID.randomUUID(),
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.open-in-view=false"
        );
    }

    @Configuration(proxyBeanMethods = false)
    static class Fixtures {
        @Bean Actions actions() { return new Actions(); }
    }

    public static class Actions {
        @Log(title = "成功操作")
        @Transactional
        public void succeed() { }

        @Log(title = "失败操作")
        @Transactional
        public void fail() { throw new IllegalArgumentException("business failed"); }
    }
}
