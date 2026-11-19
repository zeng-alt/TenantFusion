package com.github.zeng.alt.event.subscription;

import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.ObjectMapper;
import com.github.zeng.alt.event.EventDelivery;
import com.github.zeng.alt.event.EventOutcome;
import com.github.zeng.alt.event.config.EventBindingProperties;
import com.github.zeng.alt.event.context.EventContextTemplate;
import com.github.zeng.alt.event.context.RequestEventContextHandler;
import com.github.zeng.alt.event.spi.EventEndpointDto;
import com.github.zeng.alt.event.spi.EventMessageDto;
import com.github.zeng.alt.event.spi.EventMetadataDto;
import com.github.zeng.alt.event.support.EventCodec;
import com.github.zeng.alt.security.api.UserContextHolder;
import com.github.zeng.alt.tenant.api.TenantContextHolder;

import io.vavr.control.Option;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 验证业务事务和上下文先于消费确认完成。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class EventMethodHandlerTest {

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void rollsBackRetryAndRestoresWorkerBeforeReturningOutcome() throws Exception {
        final var source = new DriverManagerDataSource("jdbc:h2:mem:event_invocation;DB_CLOSE_DELAY=-1", "sa", "");
        final var jdbc = new JdbcTemplate(source);
        jdbc.execute("create table if not exists invocation_test (id int)");
        jdbc.execute("delete from invocation_test");
        final var beans = new DefaultListableBeanFactory();
        beans.registerSingleton("request", new RequestEventContextHandler());
        final var contexts = new EventContextTemplate(
                beans.getBeanProvider(com.github.zeng.alt.event.context.EventContextHandler.class)
        );
        final var codec = new EventCodec(JsonMapper.builder().build());
        final var listener = new Listener(jdbc);
        final var handler = new EventMethodHandler(
                listener,
                Listener.class.getMethod("receive", Payload.class, EventDelivery.class),
                codec,
                contexts,
                Option.of(new TransactionTemplate(new DataSourceTransactionManager(source)))
        );
        final var binding = new EventBindingProperties();
        final var endpoint = new EventEndpointDto("orders", "orders", binding);
        final EventMetadataDto metadata = EventMetadataDto.create(
                Map.of("request.tenantId", "origin", "request.userId", "42", "request.username", "alice"),
                Map.of()
        );
        final var message = new EventMessageDto(metadata, codec.encode(new Payload(1)));
        TenantContextHolder.setTenantId("worker");
        final SecurityContext originalSecurity = SecurityContextHolder.getContext();
        assertThat(handler.invoke(endpoint, message, List.of())).isInstanceOf(EventOutcome.Retry.class);
        assertThat(jdbc.queryForObject("select count(*) from invocation_test", Integer.class)).isZero();
        assertThat(TenantContextHolder.getTenantId()).isEqualTo("worker");
        assertThat(SecurityContextHolder.getContext()).isSameAs(originalSecurity);
        listener.retry = false;
        assertThat(handler.invoke(endpoint, message, List.of())).isInstanceOf(EventOutcome.Acknowledged.class);
        assertThat(jdbc.queryForObject("select count(*) from invocation_test", Integer.class)).isEqualTo(1);
    }

    /**
     * 测试监听器。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public static class Listener {
        private final JdbcTemplate jdbc;
        private boolean retry = true;

        Listener(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        public EventOutcome receive(Payload payload, EventDelivery delivery) {
            assertThat(TenantContextHolder.getTenantId()).isEqualTo("origin");
            assertThat(UserContextHolder.getId()).isEqualTo("42");
            assertThat(delivery.context().attribute("request.username").get()).isEqualTo("alice");
            jdbc.update("insert into invocation_test values (?)", payload.id());
            return retry ? delivery.retry(Duration.ofMillis(10)) : delivery.ack();
        }
    }

    /**
     * 普通业务负载。
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public record Payload(
            int id
    ) {
    }
}
