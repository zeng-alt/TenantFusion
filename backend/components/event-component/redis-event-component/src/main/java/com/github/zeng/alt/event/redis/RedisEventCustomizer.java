package com.github.zeng.alt.event.redis;

import com.github.zeng.alt.event.spi.EventEndpointDto;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.connection.stream.MapRecord;

/**
 * 贡献 Redis Streams 原生轮询容器选项。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface RedisEventCustomizer {
    void customize(EventEndpointDto endpoint,
            StreamMessageListenerContainer.StreamMessageListenerContainerOptionsBuilder<
                    String, MapRecord<String, String, String>> options);
}
