package com.github.zeng.alt.event.support;

import com.github.zeng.alt.event.spi.EventMessageDto;
import com.github.zeng.alt.event.spi.EventMetadataDto;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * 注册事件信封和持久化重试状态的 Native Image 可达性。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public class EventRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        new BindingReflectionHintsRegistrar().registerReflectionHints(hints.reflection(),
                EventMessageDto.class, EventMetadataDto.class, EventMetadataDto.RetryDto.class);
    }
}
