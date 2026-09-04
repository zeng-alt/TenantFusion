package com.github.zeng.alt.excel.read;

/**
 * 一次读取的统计与结局，独立出来是为了让 {@link ExcelReadResult} 保持三个字段。
 *
 * @param policy    本次读取用的坏行策略
 * @param totalRows 实际解析过的数据行数（不含表头），中断时是中断前的行数
 * @param errorRows 出错的行数（同一行多个约束失败只算一行）
 * @param truncated 是否因为达到 {@code alt.excel.read.max-errors} 上限而停止收集
 * @param aborted   是否整单驳回：策略为 FAIL_FAST/COLLECT_ALL 且确实有错误
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public record ExcelReadSummary(
        ExcelErrorPolicy policy, int totalRows, int errorRows, boolean truncated, boolean aborted) {

    /**
     * 空统计，用于空结果。
     *
     * @return 统计
     */
    public static ExcelReadSummary empty() {
        return new ExcelReadSummary(ExcelErrorPolicy.SKIP_ROW, 0, 0, false, false);
    }

    /**
     * 全部成功的统计。
     *
     * @param totalRows 行数
     * @return 统计
     */
    public static ExcelReadSummary allValid(int totalRows) {
        return new ExcelReadSummary(ExcelErrorPolicy.SKIP_ROW, totalRows, 0, false, false);
    }

    /**
     * 通过校验的行数。
     *
     * @return 行数
     */
    public int validRows() {
        return Math.max(0, totalRows - errorRows);
    }
}
