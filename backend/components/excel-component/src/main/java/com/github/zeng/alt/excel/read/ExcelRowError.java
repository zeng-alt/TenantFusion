package com.github.zeng.alt.excel.read;

/**
 * 单行读取失败明细。
 * <p>
 * 行号、列号均为 Excel 里肉眼看到的编号（从 1 开始），便于直接回显给用户；
 * 组件内部与 fesod 交互时用的是 0 基下标，转换在构造静态方法里完成。
 *
 * @param rowNumber    行号，从 1 开始；取不到时为 {@code -1}
 * @param columnNumber 列号，从 1 开始；非单元格级错误（整行校验）为 {@code -1}
 * @param header       出错列的表头文本，取不到时为空串
 * @param message      失败原因（校验消息或转换异常消息）
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public record ExcelRowError(int rowNumber, int columnNumber, String header, String message) {

    /** 非单元格级错误（整行校验不通过）时的列号/列名占位 */
    private static final int NO_COLUMN = -1;

    /**
     * 整行级错误，来自 Bean Validation。
     *
     * @param rowIndex 0 基行下标
     * @param message  校验消息
     * @return 错误明细
     */
    public static ExcelRowError ofRow(int rowIndex, String message) {
        return new ExcelRowError(rowIndex + 1, NO_COLUMN, "", message);
    }

    /**
     * 单元格级错误，来自类型转换。
     *
     * @param rowIndex    0 基行下标
     * @param columnIndex 0 基列下标
     * @param header      表头文本
     * @param message     失败原因
     * @return 错误明细
     */
    public static ExcelRowError ofCell(int rowIndex, int columnIndex, String header, String message) {
        return new ExcelRowError(rowIndex + 1, columnIndex + 1, header == null ? "" : header, message);
    }

    /**
     * 供日志与前端直接展示的一行文本。
     *
     * @return 形如 {@code 第 3 行第 2 列[姓名]: 不能为空}
     */
    public String describe() {
        if (columnNumber == NO_COLUMN) {
            return "第 %d 行: %s".formatted(rowNumber, message);
        }
        return "第 %d 行第 %d 列[%s]: %s".formatted(rowNumber, columnNumber, header, message);
    }
}
