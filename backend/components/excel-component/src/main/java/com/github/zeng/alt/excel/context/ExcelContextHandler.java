package com.github.zeng.alt.excel.context;

import java.util.function.Supplier;

/**
 * Excel 上下文贡献者。注册 Bean 即可参与捕获，不要求行模型继承任何类型。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface ExcelContextHandler {

    /** 在调用线程捕获上下文；快照应可重复恢复且不修改原上下文。 */
    Snapshot capture();

    /**
     * 可重复恢复的进程内上下文快照。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    @FunctionalInterface
    interface Snapshot {

        /** 恢复快照，关闭作用域时还原执行线程原状态。 */
        Scope restore();

        /** 在快照作用域内执行调用，包含异常退出时的还原。 */
        default <T> T call(Supplier<T> action) {
            try (Scope scope = restore()) {
                return action.get();
            }
        }

        /** 装饰一个任务，可交给已有线程池或调度器。 */
        default Runnable decorate(Runnable action) {
            return () -> {
                try (Scope scope = restore()) {
                    action.run();
                }
            };
        }

        /** 无上下文的快照，用于显式关闭传播。 */
        static Snapshot empty() {
            return () -> () -> { };
        }
    }

    /**
     * 执行线程的上下文恢复句柄，必须在同一线程关闭，close 实现不得抛出异常。
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
