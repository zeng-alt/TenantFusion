package com.github.zeng.alt.excel.exception;

import com.github.zeng.alt.excel.read.ExcelErrorReport;

/**
 * 导入被整单驳回时抛出，携带完整的错误报告。
 * <p>
 * 只在两种情况下抛：坏行策略是 {@code FAIL_FAST} 或 {@code COLLECT_ALL}，
 * 且参数形状是 {@code List<T>} / {@code Flux<T>} / {@code Flowable<T>}——
 * 这些形状没有地方承载失败明细，只能抛。参数形状是
 * {@code ExcelReadResult<T>} 时不抛，明细就在结果里，由调用方自己决定怎么处理。
 * <p>
 * {@link #getReport()} 拿到的报告可以直接序列化给前端，
 * 见 {@link ExcelErrorReport} 的字段说明。建议在应用的
 * {@code @RestControllerAdvice} 里把它渲染成 RFC 9457 ProblemDetail，
 * 把报告放进扩展字段。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public class ExcelValidationException extends ExcelException {

    private static final long serialVersionUID = 1L;

    private final transient ExcelErrorReport report;

    public ExcelValidationException(ExcelErrorReport report) {
        super(report.headline());
        this.report = report;
    }

    /**
     * 完整错误报告。
     *
     * @return 报告
     */
    public ExcelErrorReport getReport() {
        return report;
    }
}
