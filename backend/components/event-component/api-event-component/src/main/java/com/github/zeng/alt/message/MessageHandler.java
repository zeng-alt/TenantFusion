package com.github.zeng.alt.message;

/**
 * 消息处理器接口，类似 Spring 的 {@code ApplicationListener} 模式。
 * <p>
 * Message handler interface, following Spring's {@code ApplicationListener} pattern.
 * <p>
 * 实现此接口并注册为 Spring Bean，即可自动订阅消息。
 * 由 {@code MessageListenerBeanPostProcessor} 自动发现并注册。
 *
 * <pre>{@code
 * @Component
 * public class OrderCreatedHandler implements MessageHandler<Order> {
 *
 *     @Override
 *     public String getTopic() {
 *         return "order.created";
 *     }
 *
 *     @Override
 *     public void onMessage(Message<Order> message) {
 *         Order order = message.getPayload();
 *         System.out.println("处理订单: " + order.getId());
 *     }
 * }
 * }</pre>
 *
 * @param <T> 消息负载类型
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
public interface MessageHandler<T> {

    /**
     * 返回要订阅的主题。
     *
     * @return 主题名称
     */
    String getTopic();

    /**
     * 处理收到的消息。
     *
     * @param message 完整消息体，包含负载、头部和元数据
     */
    void onMessage(Message<T> message);
}
