package com.github.zeng.alt.excel.support;

import com.github.zeng.alt.excel.exception.ExcelReadException;

/**
 * 在专用 IO 线程上按下游需求推进同步 SAX 读取，不缓存未请求的行。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public final class ExcelDemandHelper {

    private long requested;
    private boolean cancelled;

    /** 累加下游请求；无界请求饱和到 Long.MAX_VALUE。 */
    public synchronized void request(long count) {
        requested = count >= Long.MAX_VALUE - requested ? Long.MAX_VALUE : requested + count;
        notifyAll();
    }

    /** 取消时唤醒等待中的解析线程。 */
    public synchronized void cancel() {
        cancelled = true;
        notifyAll();
    }

    /** 等待一个行额度；取消后返回 false。只能在允许阻塞的线程调用。 */
    public synchronized boolean awaitDemand() {
        try {
            while (requested == 0 && !cancelled) {
                wait();
            }
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            if (cancelled) {
                return false;
            }
            throw new ExcelReadException("等待 Excel 读取需求时被中断", failure);
        }
        if (cancelled) {
            return false;
        }
        if (requested != Long.MAX_VALUE) {
            requested--;
        }
        return true;
    }
}
