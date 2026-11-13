package com.github.zeng.alt.event.support;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.event.EventException;
import com.github.zeng.alt.event.spi.EventMessageDto;
import com.github.zeng.alt.event.spi.EventMetadataDto;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 验证 JSON 空值在传输解析边界被拒绝。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class EventCodecTest {

    private final EventCodec codec = new EventCodec(new ObjectMapper());

    @Test
    void rejectsJsonNullInsteadOfReturningNull() {
        final byte[] encoded = "null".getBytes(StandardCharsets.UTF_8);
        final var message = new EventMessageDto(EventMetadataDto.create(Map.of(), Map.of()), encoded);

        assertThatThrownBy(() -> codec.decode(encoded, EventMetadataDto.class)).isInstanceOf(EventException.class)
                .hasRootCauseMessage("Event JSON must not be null");
        assertThatThrownBy(() -> codec.payload(message, String.class)).isInstanceOf(EventException.class)
                .hasRootCauseMessage("Listener payload must not be null");
    }
}
