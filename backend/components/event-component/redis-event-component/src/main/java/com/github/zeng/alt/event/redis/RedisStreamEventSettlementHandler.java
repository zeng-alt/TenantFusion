package com.github.zeng.alt.event.redis;

import com.github.zeng.alt.event.EventOutcome;
import com.github.zeng.alt.event.config.EventBindingProperties;
import com.github.zeng.alt.event.spi.EventMessageDto;
import com.github.zeng.alt.event.spi.EventSettlementHandler;
import com.github.zeng.alt.event.support.EventCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Redis Streams 单条记录的结果处理；重投记录写入成功后才执行源 XACK。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class RedisStreamEventSettlementHandler implements EventSettlementHandler {

    private final StringRedisTemplate redis;
    private final EventCodec codec;
    private final EventBindingProperties binding;
    private final MapRecord<String, String, String> record;

    @Override
    public void ack() {
        redis.<String, String>opsForStream().acknowledge(record.getStream(), binding.getRedis().getGroup(), record.getId());
    }

    @Override
    public void retry(EventMessageDto message, EventOutcome.Retry retry) {
        append(
                binding.getRedis().getDestination() + ":retry:" + binding.getRedis().getGroup(),
                message.withMetadata(message.metadata().retryAfter(retry.delay(), retry.reason()))
        );
        ack();
    }

    @Override
    public void reject(EventMessageDto message, EventOutcome.Rejected rejected) {
        if (binding.getFailure().getAction().equals("dead-letter")) {
            append(
                    binding.getFailure().getDestination(),
                    message.withMetadata(message.metadata().rejected(rejected.reason()))
            );
        }
        ack();
    }

    private void append(String stream, EventMessageDto message) {
        redis.<String, String>opsForStream().add(
                stream,
                Map.of("event", new String(codec.encode(message), StandardCharsets.UTF_8))
        );
    }
}
