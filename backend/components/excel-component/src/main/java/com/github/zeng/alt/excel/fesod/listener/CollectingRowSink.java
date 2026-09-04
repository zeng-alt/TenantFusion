package com.github.zeng.alt.excel.fesod.listener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 把所有行收进内存，供 {@code ExcelReadSpec#execute()} 使用。
 *
 * @param <T> 行类型
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public class CollectingRowSink<T> implements ExcelRowSink<T> {

    private final List<T> rows = new ArrayList<>();

    @Override
    public void accept(T row) {
        rows.add(row);
    }

    @Override
    public long count() {
        return rows.size();
    }

    /**
     * 收集到的行。
     *
     * @return 不可变视图
     */
    public List<T> getRows() {
        return Collections.unmodifiableList(rows);
    }
}
