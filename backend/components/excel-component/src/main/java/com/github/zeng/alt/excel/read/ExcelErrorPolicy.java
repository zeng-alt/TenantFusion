package com.github.zeng.alt.excel.read;

/**
 * 坏行（解析失败或校验不通过）的处理策略。
 * <p>
 * 批量导入的三种真实诉求，之前只有一个 {@code skipInvalidRows} 布尔表达不了：
 * <ul>
 *   <li>{@link #SKIP_ROW} —— 部分成功。跳过坏行继续读，好行交给调用方，
 *       失败明细一并交出来让用户订正后补传。</li>
 *   <li>{@link #FAIL_FAST} —— 马上中断。首个坏行就停止解析，整单驳回。
 *       已执行的业务回调不会回滚。</li>
 *   <li>{@link #COLLECT_ALL} —— 校验整个文件后中断。读完全文件、收集<b>所有</b>
 *       错误再整单驳回，用户一次就能看到全部问题，不用改一行传一次。</li>
 * </ul>
 * 后两者都将有坏行的文件标记为校验失败。消费接口在 COLLECT_ALL 下先预检再消费，
 * FAIL_FAST 则边读边消费；组件不管理业务事务。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public enum ExcelErrorPolicy {

    /** 跳过坏行继续读，好行与失败明细并存 */
    SKIP_ROW,

    /** 首个坏行即中断解析，整单驳回 */
    FAIL_FAST,

    /** 读完整个文件、收集所有错误后整单驳回 */
    COLLECT_ALL;

    /**
     * 是否一遇到坏行就停止解析。
     *
     * @return true 表示中断
     */
    public boolean abortsOnFirstError() {
        return this == FAIL_FAST;
    }

    /**
     * 有错误时是否将文件标记为校验失败。
     *
     * @return true 表示整单驳回
     */
    public boolean rejectsWholeFile() {
        return this != SKIP_ROW;
    }
}
