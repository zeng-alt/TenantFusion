package com.github.zeng.alt.event.config;

import lombok.Getter;
import lombok.Setter;
import java.time.Duration;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 绑定的公共策略与中间件专属选项；不把 Kafka group 映射成 Rabbit queue。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@Getter
@Setter
public class EventBindingProperties {
    private String transport = "";
    private boolean autoStartup = true;
    private String transactionManager = "";
    private final Retry retry = new Retry();
    private final Failure failure = new Failure();
    private final Kafka kafka = new Kafka();
    private final Rabbit rabbit = new Rabbit();
    private final Redis redis = new Redis();

    public void validate() {
        Assert.hasText(transport, "binding.transport is required");
        Assert.isTrue(retry.maxAttempts > 0, "retry.max-attempts must be positive");
        Assert.isTrue(!retry.initialDelay.isNegative(), "retry.initial-delay must not be negative");
        Assert.isTrue(!retry.maxDelay.isNegative(), "retry.max-delay must not be negative");
        Assert.isTrue(Double.isFinite(retry.multiplier) && retry.multiplier >= 1, "retry.multiplier must be >= 1");
        Assert.isTrue(failure.action.equals("dead-letter") || failure.action.equals("drop"), "invalid failure.action");
    }

    public void validateSubscription() {
        if (failure.action.equals("dead-letter") && !transport.equals("redis-pubsub")) {
            Assert.hasText(failure.destination, "failure.destination is required for dead-letter");
        }
    }
    /**
     * 重试预算包含首次投递；计数随组件重新安排的消息持久化。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @Getter @Setter
    public static class Retry {
        private int maxAttempts = 3;
        private Duration initialDelay = Duration.ofSeconds(1);
        private double multiplier = 2;
        private Duration maxDelay = Duration.ofSeconds(30);
        public Duration delay(int retryCount) {
            return Duration.ofMillis((long) Math.min(maxDelay.toMillis(),
                    initialDelay.toMillis() * Math.pow(multiplier, retryCount)));
        }
    }
    /**
     * dead-letter 显式指定目的地；drop 必须显式启用。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @Getter @Setter
    public static class Failure {
        private String action = "dead-letter";
        private String destination = "";
    }
    /**
     * Kafka 专属配置，topic 与 group 保持各自语义。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @Getter @Setter
    public static class Kafka {
        private String topic = "";
        private String groupId = "";
        private String retryTopic = "";
        private int concurrency = 1;
        private Duration sendTimeout = Duration.ofSeconds(10);
    }
    /**
     * Rabbit 监听队列；发送通过 exchange/routingKey 路由。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @Getter @Setter
    public static class Rabbit {
        private String queue = "";
        private String exchange = "";
        private String routingKey = "";
        private String retryQueue = "";
        private int prefetch = 50;
        private int concurrency = 1;
        private Duration confirmTimeout = Duration.ofSeconds(10);
        public String routingKey() {
            return StringUtils.hasText(routingKey) ? routingKey : queue;
        }
    }
    /**
     * Redis 的 stream 或 channel 由 transport 明确区分。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @Getter @Setter
    public static class Redis {
        private String destination = "";
        private String group = "";
        private Duration pollInterval = Duration.ofMillis(200);
        private Duration claimIdle = Duration.ofSeconds(30);
        private int batchSize = 50;
    }
}
