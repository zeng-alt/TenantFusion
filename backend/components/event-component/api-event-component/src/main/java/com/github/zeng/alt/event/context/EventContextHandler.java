package com.github.zeng.alt.event.context;

import java.util.Map;

/**
 * 事件上下文扩展点；注册 Bean 即可参与 Spring/Modulith 与 Namastack 的捕获和恢复。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public interface EventContextHandler {

    /** 稳定且唯一的命名空间，同时用作持久化键前缀。 */
    String key();

    /** 捕获当前上下文；键值均为字符串，无上下文时返回空 Map。 */
    Map<String, String> capture();

    /** 恢复本命名空间的快照；空快照表示清空，关闭作用域时还原调用线程原状态。 */
    Scope restore(Map<String, String> contextByKey);

    /**
     * 上下文作用域，不抛出检查型异常。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @FunctionalInterface
    interface Scope extends AutoCloseable {

        @Override
        void close();
    }
}
