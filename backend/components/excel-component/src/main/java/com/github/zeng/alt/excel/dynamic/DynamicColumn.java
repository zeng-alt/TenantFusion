package com.github.zeng.alt.excel.dynamic;

import java.util.List;

/**
 * 带动态列的行。
 * <p>
 * 业务实体实现本接口（一般直接继承 {@link AbstractDynamicColumn}）后，
 * 就能用 {@code excelTemplate.readDynamic(Xxx.class)} 读取列数不定的表：
 * 能对上实体字段的列照常绑定，剩下的列变成 {@link DynamicCell} 挂进
 * {@link #getDynamicCells()}。
 * <p>
 * 导出方向由 {@link #dynamicHead()} 与 {@link #dynamicRow()} 提供表头与行数据，
 * 配合 {@code excelTemplate.writeHead(head)} 使用。
 *
 * @param <T> 单元格类型
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public interface DynamicColumn<T extends DynamicCell> {

    /**
     * 动态单元格集合，按列下标升序。
     *
     * @return 单元格集合，永不为 {@code null}
     */
    List<T> getDynamicCells();

    /**
     * 追加一个动态单元格。
     *
     * @param cell 单元格
     * @return 是否追加成功
     */
    boolean addDynamicCell(T cell);

    /**
     * 动态列部分的表头，一列一个单表头（fesod 的 {@code head} 结构是「列 → 多级表头」）。
     *
     * @return 表头结构
     */
    default List<List<String>> dynamicHead() {
        return getDynamicCells().stream()
                .map(DynamicCell::getDisplayName)
                .map(List::of)
                .toList();
    }

    /**
     * 动态列部分的一行值，顺序与 {@link #dynamicHead()} 一致。
     *
     * @return 行数据
     */
    default List<Object> dynamicRow() {
        return getDynamicCells().stream()
                .map(DynamicCell::getValue)
                .map(Object.class::cast)
                .toList();
    }
}
