package com.github.zeng.alt.excel.fesod;

import com.github.zeng.alt.excel.ExcelTemplate;
import com.github.zeng.alt.excel.dynamic.DynamicCell;
import com.github.zeng.alt.excel.dynamic.DynamicColumn;
import com.github.zeng.alt.excel.read.ExcelReadSpec;
import com.github.zeng.alt.excel.write.ExcelFillSpec;
import com.github.zeng.alt.excel.write.ExcelWriteSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.util.Assert;

import java.util.List;

/**
 * {@link ExcelTemplate} 的 fesod 实现。
 * <p>
 * 本类是无状态的，每个入口现场造一段链式配置面；可变配置全部落在那段链上，
 * 因此单例 bean 可以被并发使用。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
@RequiredArgsConstructor
public class FesodExcelTemplate implements ExcelTemplate {

    private final FesodExcelContext context;

    @Override
    public <T> ExcelReadSpec<T> read(Class<T> type) {
        Assert.notNull(type, "行类型不能为空");
        return new FesodExcelReadSpec<>(type, context, false);
    }

    @Override
    public <T extends DynamicColumn<DynamicCell>> ExcelReadSpec<T> readDynamic(Class<T> type) {
        Assert.notNull(type, "行类型不能为空");
        return new FesodExcelReadSpec<>(type, context, true);
    }

    @Override
    public <T> ExcelWriteSpec<T> write(Class<T> type) {
        Assert.notNull(type, "行类型不能为空");
        return new FesodExcelWriteSpec<>(type, null, context);
    }

    @Override
    public ExcelWriteSpec<List<Object>> writeHead(List<List<String>> head) {
        Assert.notEmpty(head, "表头不能为空");
        return new FesodExcelWriteSpec<>(null, head, context);
    }

    @Override
    public ExcelFillSpec fill(String templateLocation) {
        Assert.hasText(templateLocation, "模板路径不能为空");
        return new FesodExcelFillSpec(templateLocation, context);
    }
}
