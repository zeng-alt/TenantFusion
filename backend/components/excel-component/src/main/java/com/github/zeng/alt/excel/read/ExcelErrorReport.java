package com.github.zeng.alt.excel.read;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 给前端用的错误报告：一次导入失败的完整、可直接渲染的描述。
 * <p>
 * 设计成「直接扔给前端就能画出好界面」，所以同一份数据给了两种形态：
 * <ul>
 *   <li>{@link #errors()} —— 平铺的明细，适合渲染成一张可排序的错误表格；
 *       每条都带行号、列号、表头、字段、拒绝值、约束码。</li>
 *   <li>{@link #rows()} —— 按行分组，适合「展开某一行看它的所有问题」，
 *       或者在预览表格里给出错的单元格加红框。</li>
 * </ul>
 * 另外给了 {@link #summary()} 里的计数，前端不用自己 reduce 就能显示
 * 「共 120 行，8 行有问题，其中 3 行缺必填项」这类摘要。
 * <p>
 * 用 {@code java.util.List}/{@code Map} 而非 Vavr 集合：本类会直接出现在
 * controller 的返回值上、要过 Jackson，而本模块没有注册 {@code vavr-jackson}。
 *
 * @param fileName 出错的文件名，取不到时为空串
 * @param summary  统计与结局
 * @param errors   平铺明细，按行号、列号排序
 * @param rows     按行分组的明细，键为行号，保持行号升序
 * @param codes    各约束码的出现次数，键为 {@link ExcelRowError#code()}
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public record ExcelErrorReport(
        String fileName,
        ExcelReadSummary summary,
        List<ExcelRowError> errors,
        Map<Integer, List<ExcelRowError>> rows,
        Map<String, Integer> codes) {

    /**
     * 由读取结果生成报告。
     *
     * @param fileName 文件名，可为 {@code null}
     * @param result   读取结果
     * @return 报告
     */
    public static ExcelErrorReport of(String fileName, ExcelReadResult<?> result) {
        List<ExcelRowError> sorted = new ArrayList<>(result.errors());
        sorted.sort(Comparator.comparingInt(ExcelRowError::rowNumber)
                .thenComparingInt(ExcelRowError::columnNumber));

        Map<Integer, List<ExcelRowError>> byRow = new LinkedHashMap<>();
        Map<String, Integer> byCode = new LinkedHashMap<>();
        for (ExcelRowError error : sorted) {
            byRow.computeIfAbsent(error.rowNumber(), key -> new ArrayList<>()).add(error);
            byCode.merge(error.code(), 1, Integer::sum);
        }
        return new ExcelErrorReport(
                fileName == null ? "" : fileName,
                result.summary(),
                List.copyOf(sorted),
                Map.copyOf(byRow),
                Map.copyOf(byCode));
    }

    /**
     * 一句话摘要，适合直接放在提示条上。
     *
     * @return 形如 {@code users.xlsx: 共 120 行，8 行有问题（已中断，未导入任何数据）}
     */
    public String headline() {
        String scope = summary.aborted() ? "，已中断，未导入任何数据" : "，其余行已导入";
        String truncated = summary.truncated() ? "，错误过多已截断" : "";
        String prefix = fileName.isEmpty() ? "" : fileName + ": ";
        return "%s共 %d 行，%d 行有问题%s%s"
                .formatted(prefix, summary.totalRows(), summary.errorRows(), truncated, scope);
    }
}
