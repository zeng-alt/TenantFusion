package com.github.zeng.alt.excel.read;

/**
 * 单条读取失败明细，一个违反的约束对应一条。
 * <p>
 * 行号、列号均为 Excel 里肉眼看到的编号（从 1 开始），便于直接回显给用户；
 * 组件内部与 fesod 交互时用的是 0 基下标，转换在构造静态方法里完成。
 * <p>
 * 字段齐全是为了前端能做出好用的界面：{@code rowNumber} + {@code columnNumber}
 * 定位单元格、{@code header} 显示列名、{@code field} 对应表单字段、
 * {@code rejectedValue} 回显用户填了什么、{@code code} 让前端按约束类型分类
 * （比如「必填项缺失」和「格式不对」用不同图标），{@code message} 是兜底文案。
 *
 * @param rowNumber     行号，从 1 开始；取不到时为 {@code -1}
 * @param columnNumber  列号，从 1 开始；定位不到具体列时为 {@code -1}
 * @param header        出错列的表头文本，取不到时为空串
 * @param field         出错字段名，非字段级错误时为空串
 * @param rejectedValue 被拒绝的值，取不到时为空串
 * @param message       失败原因（校验消息或转换异常消息）
 * @param code          错误分类码：约束注解简单名，或 {@code PARSE}/{@code ROW}
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public record ExcelRowError(
        int rowNumber,
        int columnNumber,
        String header,
        String field,
        String rejectedValue,
        String message,
        String code) {

    /** 定位不到具体列时的列号占位 */
    public static final int NO_COLUMN = -1;

    /** 类型转换/解析失败的分类码 */
    public static final String CODE_PARSE = "PARSE";

    /** 无法归到某个字段的整行级错误的分类码 */
    public static final String CODE_ROW = "ROW";

    public ExcelRowError {
        header = header == null ? "" : header;
        field = field == null ? "" : field;
        rejectedValue = rejectedValue == null ? "" : rejectedValue;
        message = message == null ? "解析失败" : message;
        code = code == null ? CODE_ROW : code;
    }

    /**
     * 整行级错误：定位不到具体字段，例如整行为空或跨字段约束不通过。
     *
     * @param rowIndex 0 基行下标
     * @param message  失败原因
     * @return 错误明细
     */
    public static ExcelRowError ofRow(int rowIndex, String message) {
        return new ExcelRowError(rowIndex + 1, NO_COLUMN, "", "", "", message, CODE_ROW);
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
        return new ExcelRowError(rowIndex + 1, toColumnNumber(columnIndex), header, "", "", message, CODE_PARSE);
    }

    /**
     * 字段校验错误。
     *
     * @param rowIndex    0 基行下标
     * @param columnIndex 0 基列下标，定位不到时传 {@link #NO_COLUMN}
     * @param header      表头文本
     * @param violation   校验失败详情
     * @return 错误明细
     */
    public static ExcelRowError ofViolation(int rowIndex, int columnIndex, String header, ExcelViolation violation) {
        return new ExcelRowError(
                rowIndex + 1,
                toColumnNumber(columnIndex),
                header,
                violation.field(),
                violation.rejectedValue(),
                violation.message(),
                violation.code());
    }

    /**
     * 供日志与前端直接展示的一行文本。
     *
     * @return 形如 {@code 第 3 行第 2 列[姓名]: 不能为空}
     */
    public String describe() {
        if (columnNumber == NO_COLUMN) {
            return field.isEmpty()
                    ? "第 %d 行: %s".formatted(rowNumber, message)
                    : "第 %d 行[%s]: %s".formatted(rowNumber, field, message);
        }
        return "第 %d 行第 %d 列[%s]: %s".formatted(rowNumber, columnNumber, header, message);
    }

    private static int toColumnNumber(int columnIndex) {
        return columnIndex < 0 ? NO_COLUMN : columnIndex + 1;
    }
}
