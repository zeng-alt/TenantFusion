package com.github.zeng.alt.excel.fesod.handler;

import com.github.zeng.alt.excel.support.ExcelMessageHelper;
import org.apache.fesod.sheet.metadata.Head;
import org.apache.fesod.sheet.metadata.data.WriteCellData;
import org.apache.fesod.sheet.write.handler.CellWriteHandler;
import org.apache.fesod.sheet.write.metadata.holder.WriteSheetHolder;
import org.apache.fesod.sheet.write.metadata.holder.WriteTableHolder;
import org.apache.poi.ss.usermodel.Cell;

import java.util.List;

/**
 * 写出时把表头里的 {@code {i18n.key}} 替换成当前 Locale 的文本。
 * <p>
 * 只处理表头单元格，数据单元格原样放过。是否为 i18n key 的判断交给
 * {@link ExcelMessageHelper}，不在本类里重复维护正则。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public class I18nHeadWriteHandler implements CellWriteHandler {

    @Override
    public void afterCellDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
                                 List<WriteCellData<?>> cellDataList, Cell cell,
                                 Head head, Integer relativeRowIndex, Boolean isHead) {
        if (!Boolean.TRUE.equals(isHead) || cell == null) {
            return;
        }
        String original = cell.getStringCellValue();
        if (!ExcelMessageHelper.isCode(original)) {
            return;
        }
        cell.setCellValue(ExcelMessageHelper.resolve(original));
    }
}
