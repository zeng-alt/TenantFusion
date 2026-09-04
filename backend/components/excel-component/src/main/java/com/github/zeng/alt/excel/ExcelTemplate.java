package com.github.zeng.alt.excel;

import com.github.zeng.alt.excel.dynamic.DynamicCell;
import com.github.zeng.alt.excel.dynamic.DynamicColumn;
import com.github.zeng.alt.excel.read.ExcelReadSpec;
import com.github.zeng.alt.excel.write.ExcelFillSpec;
import com.github.zeng.alt.excel.write.ExcelWriteSpec;

import java.util.List;

/**
 * Excel 操作门面。
 * <p>
 * 本模块对外的唯一入口：五个方法各自返回一段链式配置面，Excel 引擎（当前是 fesod）
 * 只在实现类里出现，业务代码不直接接触引擎类型。
 * <p>
 * <table border="1">
 *   <caption>入口一览</caption>
 *   <tr><th>入口</th><th>用途</th></tr>
 *   <tr><td>{@link #read(Class)}</td><td>按实体读取，表头由 {@code @ExcelProperty} 声明</td></tr>
 *   <tr><td>{@link #readDynamic(Class)}</td><td>列数不定的表，固定列绑字段、其余列进 {@link DynamicCell}</td></tr>
 *   <tr><td>{@link #write(Class)}</td><td>按实体导出</td></tr>
 *   <tr><td>{@link #writeHead(List)}</td><td>无实体导出，表头与行数据都由运行期决定</td></tr>
 *   <tr><td>{@link #fill(String)}</td><td>模板填充，占位符 {@code {属性}} / {@code {.属性}}</td></tr>
 * </table>
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public interface ExcelTemplate {

    /**
     * 按实体读取。
     *
     * @param type 行类型，字段上用 {@code @ExcelProperty} 声明表头
     * @param <T>  行类型
     * @return 读取链
     */
    <T> ExcelReadSpec<T> read(Class<T> type);

    /**
     * 读取列数不定的表。
     *
     * @param type 行类型，需实现 {@link DynamicColumn}
     * @param <T>  行类型
     * @return 读取链
     */
    <T extends DynamicColumn<DynamicCell>> ExcelReadSpec<T> readDynamic(Class<T> type);

    /**
     * 按实体导出。
     *
     * @param type 行类型
     * @param <T>  行类型
     * @return 写出链
     */
    <T> ExcelWriteSpec<T> write(Class<T> type);

    /**
     * 无实体导出：表头与行数据都在运行期给出。
     * <p>
     * 导出动态列时配合 {@link DynamicColumn#dynamicHead()} 与
     * {@link DynamicColumn#dynamicRow()}：
     * <pre>{@code
     * excelTemplate.writeHead(rows.getFirst().dynamicHead())
     *         .to(outputStream)
     *         .write(rows.stream().map(DynamicColumn::dynamicRow).toList());
     * }</pre>
     *
     * @param head 表头结构，外层一个元素对应一列，内层是该列的多级表头
     * @return 写出链
     */
    ExcelWriteSpec<List<Object>> writeHead(List<List<String>> head);

    /**
     * 模板填充。
     *
     * @param templateLocation 模板在 classpath 下的相对路径，如 {@code excel/user-template.xlsx}
     * @return 填充链
     */
    ExcelFillSpec fill(String templateLocation);
}
