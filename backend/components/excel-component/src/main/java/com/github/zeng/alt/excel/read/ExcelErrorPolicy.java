package com.github.zeng.alt.excel.read;

/**
 * 坏行（解析失败或校验不通过）的处理策略。
 * <p>
 * 批量导入的三种真实诉求，之前只有一个 {@code skipInvalidRows} 布尔表达不了：
 * <ul>
 *   <li>{@link #SKIP_ROW} —— 部分成功。跳过坏行继续读，好行照常入库，
 *       失败明细一并交出来让用户订正后补传。</li>
 *   <li>{@link #FAIL_FAST} —— 马上中断。首个坏行就停止解析，整单驳回。
 *       文件很大且「有一行错就不该导」时最省资源。</li>
 *   <li>{@link #COLLECT_ALL} —— 校验整个文件后中断。读完全文件、收集<b>所有</b>
 *       错误再整单驳回，用户一次就能看到全部问题，不用改一行传一次。</li>
 * </ul>
 * 后两者都是「整单驳回」，区别只在于中断时机：要不要为了凑齐完整错误清单
 * 而把整个文件读完。
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
     * 有错误时是否整单驳回（好行也不要）。
     *
     * @return true 表示整单驳回
     */
    public boolean rejectsWholeFile() {
        return this != SKIP_ROW;
    }
}
