package com.github.zeng.alt.excel.dynamic;

import org.apache.fesod.sheet.annotation.ExcelIgnore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link DynamicColumn} 的默认基类，业务实体继承它并照常声明固定列字段即可。
 * <p>
 * 用法：
 * <pre>{@code
 * @Getter @Setter
 * public class ScoreImportCmd extends AbstractDynamicColumn<DynamicCell> {
 *     @ExcelProperty("{score.userName}")
 *     private String userName;
 *     // 其余按月份铺开的列自动进入 getDynamicCells()
 * }
 * }</pre>
 * 承载动态单元格的字段标了 {@link ExcelIgnore}，否则 fesod 会把它当成一个普通列去映射。
 *
 * @param <T> 单元格类型
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public abstract class AbstractDynamicColumn<T extends DynamicCell> implements DynamicColumn<T> {

    @ExcelIgnore
    private final List<T> dynamicCells = new ArrayList<>();

    @Override
    public List<T> getDynamicCells() {
        return Collections.unmodifiableList(dynamicCells);
    }

    @Override
    public boolean addDynamicCell(T cell) {
        return cell != null && dynamicCells.add(cell);
    }
}
