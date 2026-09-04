package com.github.zeng.alt.excel.fesod;

/**
 * 内部中断信号：坏行策略为 {@code FAIL_FAST} 时用来把 fesod 的解析循环拆掉。
 * <p>
 * 不是给使用方看的异常——{@code FesodExcelReadSpec} 会在终结步骤里捕获它并转成
 * 正常的 {@code ExcelReadResult}（{@code aborted = true}）。之所以要靠异常，
 * 是因为 fesod 的 SAX 解析没有别的中止入口；用一个专门的类型而不是复用
 * {@code ExcelReadException}，是为了不和「真的解析炸了」混在一起。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public class ExcelReadAbort extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ExcelReadAbort(String message) {
        // 中断是控制流不是故障，不需要栈轨迹
        super(message, null, false, false);
    }
}
