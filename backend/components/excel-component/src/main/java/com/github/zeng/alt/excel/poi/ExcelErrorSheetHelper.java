package com.github.zeng.alt.excel.poi;

import com.github.zeng.alt.excel.read.ExcelErrorReport;
import com.github.zeng.alt.excel.read.ExcelRowError;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * 错误定位、批注与明细表格式，样式按原样式索引复用。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
final class ExcelErrorSheetHelper {

    private final Workbook workbook;
    private final boolean highlight;
    private final boolean comments;
    private final Map<Short, CellStyle> highlightedByStyle = new HashMap<>();
    private final DataFormatter formatter = new DataFormatter();

    void annotate(Sheet sheet, ExcelErrorReport report, String summaryName) {
        final Sheet summary = workbook.createSheet(uniqueName(summaryName));
        summary.createRow(0).createCell(0).setCellValue(report.headline());
        writeRow(summary.createRow(1), List.of("工作表", "行号", "列号", "表头", "字段", "原始值", "错误原因", "错误代码"));
        report.errors().forEach(error -> {
            final String value = cellValue(sheet, error);
            writeRow(summary.createRow(summary.getLastRowNum() + 1), List.of(
                    sheet.getSheetName(), Integer.toString(error.rowNumber()), Integer.toString(error.columnNumber()),
                    error.header(), error.field(), value, error.message(), error.code()
            ));
            markCell(sheet, error);
        });
        summary.createFreezePane(0, 2);
        IntStream.range(0, 8).forEach(column -> summary.setColumnWidth(column, (column == 6 ? 60 : 22) * 256));
    }

    private String uniqueName(String requested) {
        final String base = WorkbookUtil.createSafeSheetName(requested);
        String name = base;
        int suffix = 2;
        while (workbook.getSheet(name) != null) {
            final String tail = "_" + suffix++;
            name = base.substring(0, Math.min(base.length(), 31 - tail.length())) + tail;
        }
        return name;
    }

    private String cellValue(Sheet sheet, ExcelRowError error) {
        if (!error.rejectedValue().isEmpty()) {
            return error.rejectedValue();
        }
        final Row row = error.rowNumber() > 0 ? sheet.getRow(error.rowNumber() - 1) : null;
        return row == null || error.columnNumber() < 1 ? ""
                : formatter.formatCellValue(row.getCell(error.columnNumber() - 1));
    }

    private void markCell(Sheet sheet, ExcelRowError error) {
        if ((!highlight && !comments) || error.rowNumber() < 1
                || error.rowNumber() > workbook.getSpreadsheetVersion().getMaxRows()
                || error.columnNumber() > workbook.getSpreadsheetVersion().getMaxColumns()) {
            return;
        }
        final int rowIndex = error.rowNumber() - 1;
        final Row row = sheet.getRow(rowIndex) == null ? sheet.createRow(rowIndex) : sheet.getRow(rowIndex);
        final Cell cell = row.getCell(
                Math.max(0, error.columnNumber() - 1), Row.MissingCellPolicy.CREATE_NULL_AS_BLANK
        );
        if (highlight) {
            cell.setCellStyle(highlightedByStyle.computeIfAbsent(cell.getCellStyle().getIndex(), index -> {
                final CellStyle style = workbook.createCellStyle();
                style.cloneStyleFrom(cell.getCellStyle());
                style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                return style;
            }));
            highlightedByStyle.put(cell.getCellStyle().getIndex(), cell.getCellStyle());
        }
        if (comments) {
            appendComment(sheet, cell, error.message());
        }
    }

    private void appendComment(Sheet sheet, Cell cell, String message) {
        final Comment comment = cell.getCellComment();
        if (comment != null) {
            comment.setString(workbook.getCreationHelper().createRichTextString(
                    comment.getString().getString() + "\n" + message
            ));
            return;
        }
        final ClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();
        anchor.setCol1(cell.getColumnIndex());
        anchor.setCol2(Math.min(cell.getColumnIndex() + 3, workbook.getSpreadsheetVersion().getMaxColumns() - 1));
        anchor.setRow1(cell.getRowIndex());
        anchor.setRow2(Math.min(cell.getRowIndex() + 3, workbook.getSpreadsheetVersion().getMaxRows() - 1));
        final Comment created = sheet.createDrawingPatriarch().createCellComment(anchor);
        created.setAuthor("Excel 校验");
        created.setString(workbook.getCreationHelper().createRichTextString(message));
        cell.setCellComment(created);
    }

    private void writeRow(Row row, List<String> values) {
        IntStream.range(0, values.size()).forEach(index -> row.createCell(index).setCellValue(values.get(index)));
    }
}
