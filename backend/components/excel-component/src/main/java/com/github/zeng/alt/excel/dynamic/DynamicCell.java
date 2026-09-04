package com.github.zeng.alt.excel.dynamic;

import com.github.zeng.alt.core.validation.TypeEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 动态列的一个单元格。
 * <p>
 * 「动态列」指列数与列名在编译期未知、由业务数据决定的表（例如按月份铺开的考核表）。
 * 固定列仍然映射到实体字段，剩下的列以本类的形式挂在
 * {@link DynamicColumn#getDynamicCells()} 上。
 * <p>
 * 实现 {@link TypeEntity}，因此可以用 {@link TypeEntity#getTargetValue()} 按
 * {@code type} 把字符串值转成目标类型。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
@Getter
@Setter
public class DynamicCell implements TypeEntity {

    /** 0 基列下标 */
    private Integer columnIndex;

    /** 表头原文；若原文是 {@code {i18n.key}} 形式，这里保留 key 本身 */
    private String name;

    /** 表头解析后的展示文本；{@code name} 不是 i18n key 时与 {@code name} 相同 */
    private String displayName;

    /** 单元格值，统一以字符串承载 */
    private String value;

    /** 目标类型标识，交给 {@link TypeEntity#getTargetValue()} 做转换 */
    private String type;

    /**
     * 构造一个动态单元格。
     *
     * @param columnIndex 0 基列下标
     * @param name        表头原文
     * @param displayName 表头展示文本
     * @param value       单元格值
     * @return 单元格
     */
    public static DynamicCell of(Integer columnIndex, String name, String displayName, String value) {
        DynamicCell cell = new DynamicCell();
        cell.setColumnIndex(columnIndex);
        cell.setName(name);
        cell.setDisplayName(displayName);
        cell.setValue(value);
        return cell;
    }
}
