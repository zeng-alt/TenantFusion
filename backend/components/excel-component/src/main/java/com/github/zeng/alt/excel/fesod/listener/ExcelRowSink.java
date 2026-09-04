package com.github.zeng.alt.excel.fesod.listener;

/**
 * 解析出的行的去处。
 * <p>
 * 把「怎么解析」（监听器）和「解析完给谁」（终结步骤）拆开：三个终结步骤
 * ——全量收集、逐行消费、下发到 {@code Flowable}——各自是一个 sink，
 * 与两种监听器（fesod 原生模型 / 国际化表头绑定）自由组合，避免 2×3 的类爆炸。
 *
 * @param <T> 行类型
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public interface ExcelRowSink<T> {

    /**
     * 接收一行。
     *
     * @param row 数据行
     */
    void accept(T row);

    /**
     * 全部行下发完毕。
     */
    default void complete() {
    }

    /**
     * 下游是否已经不要数据了（{@code Flowable} 被取消）。
     * <p>
     * 监听器每行都会问一次，返回 {@code true} 时停止解析剩余行。
     *
     * @return true 表示已取消
     */
    default boolean isCancelled() {
        return false;
    }

    /**
     * 已接收的行数。
     *
     * @return 行数
     */
    long count();
}
