package com.github.zeng.alt.excel.read;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link ExcelReadResult} 的合并工具，内部便利类。
 * <p>
 * 单独一个类而不是 {@code ExcelReadResult} 上的实例方法：合并只在「一个参数收了
 * 多个上传文件」这一个场景用到，不该出现在结果对象的公开面上。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public final class ExcelReadResults {

    private ExcelReadResults() {
    }

    /**
     * 合并两次读取的结果。
     * <p>
     * 统计相加；结局取「更严格」的一方——任一份文件被驳回，整批就该驳回；
     * 截断标记同理取或。策略以左侧为准（同一个参数上的多个文件用的是同一套策略）。
     *
     * @param left  左值
     * @param right 右值
     * @param <T>   行类型
     * @return 合并后的结果
     */
    public static <T> ExcelReadResult<T> merge(ExcelReadResult<T> left, ExcelReadResult<T> right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        List<T> rows = new ArrayList<>(left.rows());
        rows.addAll(right.rows());
        List<ExcelRowError> errors = new ArrayList<>(left.errors());
        errors.addAll(right.errors());

        ExcelReadSummary a = left.summary();
        ExcelReadSummary b = right.summary();
        ExcelReadSummary summary = new ExcelReadSummary(
                b.policy() == ExcelErrorPolicy.SKIP_ROW ? a.policy() : b.policy(),
                a.totalRows() + b.totalRows(),
                a.errorRows() + b.errorRows(),
                a.truncated() || b.truncated(),
                a.aborted() || b.aborted());
        return new ExcelReadResult<>(rows, errors, summary);
    }
}
