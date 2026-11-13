package com.github.zeng.alt.event;

import org.springframework.util.Assert;

import java.time.Duration;
import java.util.Objects;

/**
 * 消费结果；只有调用及托管事务结束后才由传输适配器执行。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public sealed interface EventOutcome {
    /**
     * 成功结果，不立即操作 Broker。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    record Acknowledged() implements EventOutcome {
    }

    /**
     * 重试请求；延迟是最小等待时间。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    record Retry(
            Duration delay,
            String reason
    ) implements EventOutcome {
        public Retry {
            Objects.requireNonNull(delay, "delay");
            Assert.isTrue(!delay.isNegative(), "retry delay must not be negative");
            Objects.requireNonNull(reason, "reason");
        }
    }

    /**
     * 不再重试，进入端点配置的失败处理。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    record Rejected(
            String reason
    ) implements EventOutcome {
        public Rejected {
            Assert.hasText(reason, "rejection reason must not be blank");
        }
    }
}
