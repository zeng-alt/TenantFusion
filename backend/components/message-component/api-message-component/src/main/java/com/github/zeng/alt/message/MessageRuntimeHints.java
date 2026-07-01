package com.github.zeng.alt.message;

import com.github.zeng.alt.message.codec.JacksonMessagePacketCodec;
import com.github.zeng.alt.message.codec.MessagePacket;
import com.github.zeng.alt.message.codec.MessagePacketCodec;
import com.github.zeng.alt.message.exception.MessageException;
import com.github.zeng.alt.message.subscription.MessageListenerBeanPostProcessor;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM native image runtime hints for the message component.
 * <p>
 * Registers reflection hints for all API message-component classes
 * required to work correctly in a native image.
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
public class MessageRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // ========== Annotation ==========
        registerType(hints, com.github.zeng.alt.message.annotation.MessageListener.class);

        // ========== Core API ==========
        registerType(hints,
                Message.class,
                MessageSender.class,
                com.github.zeng.alt.message.MessageListener.class,
                MessageHandler.class,
                MessageQueueTemplate.class);

        // ========== Codec / Packet ==========
        registerType(hints,
                MessagePacket.class,
                MessagePacketCodec.class,
                JacksonMessagePacketCodec.class);

        // ========== Subscription ==========
        registerType(hints,
                MessageListenerBeanPostProcessor.class);

        // ========== Exception ==========
        registerType(hints,
                MessageException.class);
    }

    private static void registerType(RuntimeHints hints, Class<?>... classes) {
        for (Class<?> clazz : classes) {
            hints.reflection().registerType(clazz,
                    MemberCategory.INTROSPECT_DECLARED_METHODS,
                    MemberCategory.DECLARED_FIELDS,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS,
                    MemberCategory.DECLARED_FIELDS);
        }
    }
}
