package com.github.zeng.alt.excel.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fesod.sheet.context.AnalysisContext;
import org.apache.fesod.sheet.read.listener.ReadListener;

/**
 * 单行解析出错时跳过该行、继续读取后续数据的读监听器。
 * <p>
 * {@code ReadListener#onException} 的默认实现会把异常继续抛出，导致整份文件的读取中断；
 * 批量导入场景通常希望「坏行跳过、好行照收」，最后再统一汇报失败明细。
 * <p>
 * 本接口在包重命名时丢失（{@code AbstractReadListener} 仍在 implements 列表里引用它，
 * 但全仓已无实现，模块因此编译不过）。按同包 {@link I18nReadListener}、
 * {@link ValidaReadListener} 的形态重建。
 *
 * @param <T> 单行数据类型
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public interface IgnoreExceptionReadListener<T> extends ReadListener<T> {

    /** 接口上不能用 Lombok 的日志注解，显式声明 */
    Log LOG = LogFactory.getLog(IgnoreExceptionReadListener.class);

    /**
     * 吞掉单行异常并记录日志，使读取得以继续。
     *
     * @param exception 解析该行时抛出的异常
     * @param context   解析上下文
     * @throws Exception 保留父接口的签名，便于实现类按需继续抛出；本默认实现不抛
     */
    @Override
    default void onException(Exception exception, AnalysisContext context) throws Exception {
        LOG.warn("解析第 " + rowIndexOf(context) + " 行失败，已跳过该行", exception);
    }

    /**
     * 取当前行号，取不到时返回 -1。
     *
     * @param context 解析上下文
     * @return 行号
     */
    private static int rowIndexOf(AnalysisContext context) {
        try {
            return context.readRowHolder().getRowIndex();
        } catch (RuntimeException e) {
            return -1;
        }
    }
}
