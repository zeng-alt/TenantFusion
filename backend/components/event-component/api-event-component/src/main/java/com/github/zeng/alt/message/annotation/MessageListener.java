package com.github.zeng.alt.message.annotation;

import java.lang.annotation.*;

/**
 * 消息监听器注解，标注在 Spring Bean 的方法上以实现消息订阅。
 * <p>
 * Message listener annotation for subscribing to message topics
 * via annotated methods on Spring beans.
 * <p>
 * <b>支持的方法签名:</b>
 * <pre>{@code
 * // 仅负载参数（推荐）
 * @MessageListener(topic = "order.created")
 * public void handleOrder(Order order) { }
 *
 * // 完整 Message 参数
 * @MessageListener(topic = "order.created")
 * public void handleOrder(Message<Order> message) { }
 *
 * // 无参数
 * @MessageListener(topic = "order.created")
 * public void handleOrder() { }
 * }</pre>
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface MessageListener {

    /**
     * 订阅的主题（支持 SpEL 表达式：{@code "${some.property}"} 从配置读取）。
     *
     * @return 主题名称
     */
    String topic();
}
