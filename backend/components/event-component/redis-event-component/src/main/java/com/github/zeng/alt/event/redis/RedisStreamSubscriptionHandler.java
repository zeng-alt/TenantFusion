package com.github.zeng.alt.event.redis;

import com.github.zeng.alt.event.EventException;
import com.github.zeng.alt.event.config.EventBindingProperties;
import com.github.zeng.alt.event.spi.EventConsumer;
import com.github.zeng.alt.event.spi.EventEndpointDto;
import com.github.zeng.alt.event.spi.EventMessageDto;
import com.github.zeng.alt.event.spi.EventSubscriptionHandler;
import com.github.zeng.alt.event.support.EventCodec;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.vavr.control.Option;
import io.vavr.control.Try;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.ConsumerStreamReadRequest;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptionsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 使用原生 Streams 容器消费，并恢复达到空闲阈值的 pending 消息；重试内容保存在独立流中。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public class RedisStreamSubscriptionHandler implements EventSubscriptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RedisStreamSubscriptionHandler.class);
    private final StringRedisTemplate redis;
    private final EventCodec codec;
    private final EventEndpointDto endpoint;
    private final EventConsumer listener;
    private final Consumer consumer;
    private final String retryStream;
    private final StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private Option<Disposable> recovery = Option.none();
    private boolean initialized;

    public RedisStreamSubscriptionHandler(
            StringRedisTemplate redis,
            EventCodec codec,
            ObjectProvider<RedisEventCustomizer> customizers,
            EventEndpointDto endpoint,
            EventConsumer listener
    ) {
        this.redis = redis;
        this.codec = codec;
        this.endpoint = endpoint;
        this.listener = listener;
        final EventBindingProperties.Redis options = endpoint.binding().getRedis();
        this.consumer = Consumer.from(options.getGroup(), endpoint.id() + "-" + UUID.randomUUID());
        this.retryStream = options.getDestination() + ":retry:" + options.getGroup();
        final StreamMessageListenerContainerOptionsBuilder<String, MapRecord<String, String, String>> builder =
                StreamMessageListenerContainerOptions.builder()
                        .batchSize(options.getBatchSize())
                        .pollTimeout(options.getPollInterval());
        customizers.orderedStream().forEach(customizer -> customizer.customize(endpoint, builder));
        this.container = StreamMessageListenerContainer.create(redis.getRequiredConnectionFactory(), builder.build());
    }

    @Override
    public synchronized void start() {
        if (container.isRunning()) {
            return;
        }
        if (!initialized) {
            List.of(endpoint.binding().getRedis().getDestination(), retryStream).forEach(this::register);
            initialized = true;
        }
        container.start();
        recovery = Option.of(
                Flowable.interval(
                        endpoint.binding().getRedis().getPollInterval().toMillis(),
                        TimeUnit.MILLISECONDS,
                        Schedulers.io()
                ).subscribe(ignored -> recoverPending())
        );
    }

    private void register(String stream) {
        Try.of(() -> redis.opsForStream().createGroup(stream, ReadOffset.from("0-0"), consumer.getGroup()))
                .recoverWith(failure -> {
                    final String reason = String
                            .valueOf(NestedExceptionUtils.getMostSpecificCause(failure).getMessage());
                    return reason.contains("BUSYGROUP") ? Try.success(consumer.getGroup()) : Try.failure(failure);
                }).get();
        final ConsumerStreamReadRequest<String> request = StreamMessageListenerContainer.StreamReadRequest
                .builder(StreamOffset.create(stream, ReadOffset.lastConsumed())).consumer(consumer)
                .autoAcknowledge(false).cancelOnError(failure -> false)
                .errorHandler(failure -> LOG.warn("Redis event delivery failed for {}", endpoint.id(), failure))
                .build();
        container.register(request, this::receive);
    }

    private void receive(MapRecord<String, String, String> record) {
        final String encoded = record.getValue().get("event");
        if (encoded == null) {
            throw new EventException("Redis Stream entry has no event field");
        }
        final EventMessageDto message = codec.decode(encoded.getBytes(StandardCharsets.UTF_8), EventMessageDto.class);
        if (message.metadata().retry().notBefore() > System.currentTimeMillis()) {
            return;
        }
        final var settlement = new RedisStreamEventSettlementHandler(redis, codec, endpoint.binding(), record);
        settlement.settle(message, listener.consume(message, List.of(record)));
    }

    private void recoverPending() {
        Try.run(
                () -> List.of(endpoint.binding().getRedis().getDestination(), retryStream).forEach(this::recoverPending)
        ).onFailure(failure -> LOG.warn("Redis pending recovery failed for {}", endpoint.id(), failure));
    }

    private void recoverPending(String stream) {
        final EventBindingProperties.Redis options = endpoint.binding().getRedis();
        Option.of(
                redis.opsForStream().pending(
                        stream,
                        consumer.getGroup(),
                        Range.unbounded(),
                        options.getBatchSize(),
                        options.getClaimIdle()
                )
        ).filter(pending -> !pending.isEmpty()).peek(pending -> {
            final RecordId[] ids = pending.stream().map(PendingMessage::getId).toArray(RecordId[]::new);
            redis.<String, String>opsForStream()
                    .claim(stream, consumer.getGroup(), consumer.getName(), options.getClaimIdle(), ids)
                    .forEach(this::receive);
        });
    }

    @Override
    public synchronized void stop() {
        recovery.peek(Disposable::dispose);
        recovery = Option.none();
        container.stop();
    }

    @Override
    public boolean isRunning() {
        return container.isRunning();
    }

    @Override
    public boolean isAutoStartup() {
        return endpoint.binding().isAutoStartup();
    }
}
